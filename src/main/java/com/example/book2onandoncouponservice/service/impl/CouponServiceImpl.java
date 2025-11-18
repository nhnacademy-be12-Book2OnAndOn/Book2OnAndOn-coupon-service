package com.example.book2onandoncouponservice.service.impl;

import com.example.book2onandoncouponservice.dto.response.CouponResponseDto;
import com.example.book2onandoncouponservice.entity.Coupon;
import com.example.book2onandoncouponservice.entity.CouponPolicy;
import com.example.book2onandoncouponservice.repository.CouponPolicyRepository;
import com.example.book2onandoncouponservice.repository.CouponRepository;
import com.example.book2onandoncouponservice.service.CouponService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class CouponServiceImpl implements CouponService {
    private final CouponRepository couponRepository;
    private final CouponPolicyRepository couponPolicyRepository;

    //쿠폰 발급
    @Transactional
    @Override
    public Long issueCoupon(Long userId, Long policyId) {

        //정책조회
        CouponPolicy couponPolicy = couponPolicyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 정책"));

        //쿠폰 중복 발급 체크
        if (couponRepository.existsByUserIdAndCouponPolicy_CouponPolicyId(userId, policyId)) {
            throw new RuntimeException("이미 발급받은 쿠폰입니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expirationDate = calculateExpirationDate(couponPolicy, now);

        Coupon coupon = new Coupon(
                couponPolicy.getCouponPolicyName(),
                userId,
                couponPolicy,
                now,
                expirationDate
        );

        return couponRepository.save(coupon).getCouponId();
    }

    //특정 사용자 쿠폰조회
    @Override
    public List<CouponResponseDto> getMyCoupons(Long userId) {

        List<Coupon> coupons = couponRepository.findByUserId(userId);

        return coupons.stream()
                .map(CouponResponseDto::new)
                .collect(Collectors.toList());
    }


    //쿠폰사용
    @Transactional
    @Override
    public void useCoupon(Long couponId, Long orderId, Long userId) {

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 쿠폰입니다."));

        //쿠폰 소유자인지 확인
        if (!coupon.getUserId().equals(userId)) {
            throw new RuntimeException("해당 쿠폰의 소유자가 아닙니다.");
        }

        coupon.use(orderId);
    }

    //쿠폰사용 취소
    @Transactional
    @Override
    public void cancelCouponUsage(Long orderId) {

        Coupon coupon = couponRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("해당 주문에 사용된 쿠폰이 아닙니다."));

        //쿠폰 취소 메서드 호출
        coupon.cancelUsage();
    }

    private LocalDateTime calculateExpirationDate(CouponPolicy couponPolicy, LocalDateTime now) {
        //고정 기간 정책인지 확인
        if (couponPolicy.getFixedEndDate() != null) {
            // ex) 2024-12-31 -> 2024-12-31 23:59:59.999999
            // LocalTime.MAX 사용불가 MySQL의 DATETIME이 소수초를 6자리까지만 지원 / 나중에 DB에서 DATETIME fsp값 고려, 학습 필요
            return couponPolicy.getFixedEndDate().atTime(23, 59, 59, 999999000);
        }

        //유효 기간 정책인지 확인
        if (couponPolicy.getDurationDays() != null) {
            return now.plusDays(couponPolicy.getDurationDays());
        }

        throw new IllegalStateException("쿠폰 정책에 만료일 기준(FixedDate 또는 Duration)이 없습니다.");
    }
}
