package com.example.book2onandoncouponservice.service;

import com.example.book2onandoncouponservice.dto.response.CouponResponseDto;
import java.util.List;

public interface CouponService {

    //쿠폰 발급
    Long issueCoupon(Long userId, Long policyId);

    //보유중인 쿠폰 조회
    List<CouponResponseDto> getMyCoupons(Long userId);

    //쿠폰 사용
    void useCoupon(Long couponId, Long orderId, Long userId);

    //쿠폰 사용 취소 (결제 오류, 반품)
    void cancelCouponUsage(Long orderId);
}