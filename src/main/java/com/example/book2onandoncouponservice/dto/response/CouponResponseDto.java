package com.example.book2onandoncouponservice.dto.response;

import com.example.book2onandoncouponservice.entity.Coupon;
import com.example.book2onandoncouponservice.entity.CouponPolicy;
import com.example.book2onandoncouponservice.entity.CouponPolicyDiscountType;
import com.example.book2onandoncouponservice.entity.CouponPolicyStatus;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CouponResponseDto {

    private Long couponId;
    private String couponName;
    private String discountDescription;
    private Integer discountValue;
    private CouponPolicyDiscountType discountType;

    private Integer minPrice;
    private Integer maxPrice;

    private Integer durationDays;       // 상대 기간 (null 가능)
    private LocalDate startDate;        // 절대 시작일 (null 가능)
    private LocalDate endDate;          // 절대 종료일 (null 가능)

    private CouponPolicyStatus status;

    private Integer couponRemainingQuantity;  // 남은 재고(null = 무제한)

    // Entity -> DTO 변환
    public CouponResponseDto(Coupon coupon) {
        CouponPolicy policy = coupon.getCouponPolicy();

        // 할인 설명
        String discountDescription;
        if (policy.getCouponPolicyDiscountType() == CouponPolicyDiscountType.FIXED) {
            discountDescription = String.format("%,d원 할인", policy.getCouponDiscountValue());
        } else {
            discountDescription = String.format("%d%% 할인", policy.getCouponDiscountValue());
        }

        this.couponId = coupon.getCouponId();
        this.couponName = policy.getCouponPolicyName();
        this.discountDescription = discountDescription;
        this.discountValue = policy.getCouponDiscountValue();
        this.discountType = policy.getCouponPolicyDiscountType();
        this.minPrice = policy.getMinPrice();
        this.maxPrice = policy.getMaxPrice();
        this.status = policy.getCouponPolicyStatus();

        // 상대 유효기간 vs 절대 유효기간
        this.durationDays = policy.getDurationDays(); // null이면 상대 기간 없음

        if (policy.getCouponPolicyDiscountType() == CouponPolicyDiscountType.FIXED) {
            // 절대 유효기간
            this.startDate = policy.getFixedStartDate();
            this.endDate = policy.getFixedEndDate();
        } else {
            // 상대 기간 쿠폰이면 절대 날짜 없음
            this.startDate = null;
            this.endDate = null;
        }

        // 재고 기반 구조: 남은 재고(remainingQuantity)
        this.couponRemainingQuantity = coupon.getCouponRemainingQuantity();
    }
}
