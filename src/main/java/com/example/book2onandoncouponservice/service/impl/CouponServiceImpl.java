package com.example.book2onandoncouponservice.service.impl;

import com.example.book2onandoncouponservice.dto.request.CouponCreateRequestDto;
import com.example.book2onandoncouponservice.dto.response.CouponResponseDto;
import com.example.book2onandoncouponservice.entity.Coupon;
import com.example.book2onandoncouponservice.entity.CouponPolicy;
import com.example.book2onandoncouponservice.entity.CouponPolicyStatus;
import com.example.book2onandoncouponservice.entity.CouponPolicyType;
import com.example.book2onandoncouponservice.entity.MemberCoupon;
import com.example.book2onandoncouponservice.repository.CouponPolicyRepository;
import com.example.book2onandoncouponservice.repository.CouponRepository;
import com.example.book2onandoncouponservice.repository.MemberCouponRepository;
import com.example.book2onandoncouponservice.service.CouponService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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

        Coupon coupon = new Coupon(requestDto.getCouponRemainingQuantity(), policy);
        Coupon savedCoupon = couponRepository.save(coupon);
        return savedCoupon.getCouponId();
    }

    // 전체 쿠폰 조회
    @Transactional(readOnly = true)
    @Override
    public Page<CouponResponseDto> getCoupons(Pageable pageable) {
        Page<Coupon> coupons = couponRepository.findAll(pageable);
        return coupons.map(CouponResponseDto::new);
    }


    // 쿠폰 상세 조회
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
        Page<Coupon> coupons = couponRepository.findAvailableCoupons(
                CouponPolicyStatus.ACTIVE,
                today,
                pageable
        );
        return coupons.map(CouponResponseDto::new);
    }

    @Transactional
    @Override
    public Long issueMemberCoupon(Long userId, Long couponId) {

        Coupon coupon = couponRepository.findByIdForUpdate(couponId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 쿠폰입니다."));

        CouponPolicy policy = coupon.getCouponPolicy();

        if (!policy.isIssuable()) {
            throw new RuntimeException("종료된 정책입니다.");
        }

        if (memberCouponRepository.existsByUserIdAndCoupon_CouponId(userId, couponId)) {
            throw new RuntimeException("이미 발급받은 쿠폰입니다.");
        }

        coupon.decreaseStock();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = calculateExpirationDate(policy, now);

        MemberCoupon memberCoupon = new MemberCoupon(
                userId,
                coupon,
                now,
                endDate
        );

        MemberCoupon savedMemberCoupon = memberCouponRepository.save(memberCoupon);

        return savedMemberCoupon.getMemberCouponId();
    }

    //쿠폰 수량 업데이트
    @Transactional
    @Override
    public Integer updateAccount(Long couponId, Integer quantity) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 쿠폰입니다."));

        coupon.update(quantity);

        return quantity;
    }

    //회원가입 시 웰컴쿠폰 지급
    @Transactional
    public void issueWelcomeCoupon(Long userId) {
        CouponPolicy welcomePolicy = policyRepository.findByCouponPolicyType(CouponPolicyType.WELCOME)
                .orElseThrow(() -> new RuntimeException("WelcomeCoupon 정책이 존재하지 않습니다."));
        Coupon welcomeCoupon = couponRepository.findByCouponPolicy_CouponPolicyId(welcomePolicy.getCouponPolicyId())
                .orElseThrow(() -> new RuntimeException("Welcome Coupon이 존재하지 않습니다."));

        issueMemberCoupon(userId, welcomeCoupon.getCouponId());

        log.info("웰컴 쿠폰 지급 성공 userId = {}", userId);
    }

    //만료일 계산
    private LocalDateTime calculateExpirationDate(CouponPolicy policy, LocalDateTime now) {
        if (policy.getFixedEndDate() != null) {
            return policy.getFixedEndDate().atTime(23, 59, 59, 999999000);
        }
        if (policy.getDurationDays() != null) {
            return now.plusDays(policy.getDurationDays());
        }
        throw new IllegalStateException("쿠폰 정책에 만료일 기준이 없습니다. policyId=" + policy.getCouponPolicyId());
    }
}
