package com.example.book2onandoncouponservice.service.impl;

import com.example.book2onandoncouponservice.dto.request.CouponCreateRequestDto;
import com.example.book2onandoncouponservice.dto.response.CouponResponseDto;
import com.example.book2onandoncouponservice.entity.Coupon;
import com.example.book2onandoncouponservice.entity.CouponPolicy;
import com.example.book2onandoncouponservice.entity.CouponPolicyStatus;
import com.example.book2onandoncouponservice.entity.MemberCoupon;
import com.example.book2onandoncouponservice.repository.CouponPolicyRepository;
import com.example.book2onandoncouponservice.repository.CouponRepository;
import com.example.book2onandoncouponservice.repository.MemberCouponRepository;
import com.example.book2onandoncouponservice.service.CouponService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class CouponServiceImpl implements CouponService {
    private final CouponPolicyRepository policyRepository;
    private final CouponRepository couponRepository;
    private final MemberCouponRepository memberCouponRepository;

    @Transactional
    @Override
    public Long createCouponUnit(CouponCreateRequestDto requestDto) {
        CouponPolicy policy = policyRepository.findById(requestDto.getCouponPolicyId())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 정책"));

        Coupon coupon = new Coupon(requestDto.getCoupon_total_quantity(), policy);
        Coupon saveCoupon = couponRepository.save(coupon);

        return saveCoupon.getCouponId();
    }

    @Transactional(readOnly = true)
    @Override
    public Page<CouponResponseDto> getCoupons(Pageable pageable) {

        Page<Coupon> coupons = couponRepository.findAll(pageable);

        return coupons.map(CouponResponseDto::new);
    }

    @Override
    public CouponResponseDto getCouponDetail(Long couponId) {

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 쿠폰입니다."));

        return new CouponResponseDto(coupon);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<CouponResponseDto> getAvailableCoupon(Pageable pageable) {
        LocalDate today = LocalDate.now();

        // 정책 상태, 재고 남았는지, 기간 유효한지 DB 쿼리에서 필터링
        Page<Coupon> coupons = couponRepository.findByCouponPolicy_CouponPolicyStatusAndCouponIssueCountLessThanAndCouponPolicy_FixedEndDateGreaterThanEqual(
                CouponPolicyStatus.ACTIVE, //CouponPolicyStatus
                0, //CouponIssueCountLessThanAndCouponPolicy
                today, //CouponPolicy_FixedEndDateGreaterThanEqual
                pageable
        );

        return coupons.map(CouponResponseDto::new);
    }

    @Transactional
    @Override
    public Long issueMemberCoupon(Long userId, Long couponId) {

        Coupon coupon = couponRepository.findByIdForUpdate(couponId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 쿠폰입니다."));

        CouponPolicy couponPolicy = coupon.getCouponPolicy();

        //유효한 정책인지 검증
        if (!couponPolicy.isIssuable()) {
            throw new RuntimeException("종료된 정책입니다.");
        }

        //중복발급 체크
        if (memberCouponRepository.existsByUserIdAndCoupon_CouponId(userId, couponId)) {
            throw new RuntimeException("이미 발급받은 쿠폰입니다.");
        }

        coupon.issue();

        //만료일 계산
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = calculateExpirationDate(couponPolicy, now);

        MemberCoupon memberCoupon = new MemberCoupon(
                userId,
                coupon,
                now,
                endDate
        );

        MemberCoupon saveMemberCoupon = memberCouponRepository.save(memberCoupon);

        return saveMemberCoupon.getMemberCouponId();
    }

    private LocalDateTime calculateExpirationDate(CouponPolicy policy, LocalDateTime now) {
        if (policy.getFixedEndDate() != null) {
            // 고정 만료일 fixedEndDate
            return policy.getFixedEndDate().atTime(23, 59, 59, 999999000);
        }
        if (policy.getDurationDays() != null) {
            // 유효 기간 정책
            return now.plusDays(policy.getDurationDays());
        }
        throw new IllegalStateException("쿠폰 정책에 만료일 기준이 없습니다. policyId=" + policy.getCouponPolicyId());
    }
}
