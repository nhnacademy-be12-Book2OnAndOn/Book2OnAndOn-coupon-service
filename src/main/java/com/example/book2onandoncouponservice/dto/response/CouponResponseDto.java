package com.example.book2onandoncouponservice.dto.response;

import com.example.book2onandoncouponservice.entity.Coupon;
import com.example.book2onandoncouponservice.entity.CouponDiscountType;
import com.example.book2onandoncouponservice.entity.CouponPolicy;
import com.example.book2onandoncouponservice.entity.CouponStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CouponResponseDto {
    private Long couponId;
    private String couponName; // 정책 이름 또는 개별 쿠폰 이름
    private String discountDescription; // 예: "10% 할인" 또는 "3000원 할인"
    private Integer discountValue;
    private CouponDiscountType discountType;
    private Integer minPrice; // 사용 조건
    private Integer maxPrice; // 최대 할인 한도
    private LocalDateTime issuedDate;
    private LocalDateTime endDate; // 만료일
    private CouponStatus status; // 사용 가능 여부

    // Entity -> DTO 변환 메서드
    public CouponResponseDto(Coupon coupon) {
        CouponPolicy couponPolicy = coupon.getCouponPolicy();

        String discountDescription;
        if (couponPolicy.getCouponDiscountType().equals(CouponDiscountType.FIXED)) {
            discountDescription = String.format("%,d원 할인", couponPolicy.getCouponDiscountValue().intValue());
        } else {
            discountDescription = String.format("%.0f%% 할인", couponPolicy.getCouponDiscountValue());
        }

        this.couponId = coupon.getCouponId();
        this.couponName = couponPolicy.getCouponPolicyName();
        this.discountDescription = discountDescription; // 계산된 설명 할당
        this.discountValue = couponPolicy.getCouponDiscountValue();
        this.discountType = couponPolicy.getCouponDiscountType();
        this.minPrice = couponPolicy.getMinPrice();
        this.maxPrice = couponPolicy.getMaxPrice();
        this.issuedDate = coupon.getIssuedDate();
        this.endDate = coupon.getEndDate();
        this.status = coupon.getCouponStatus();
    }
}