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

    //특정 사용자 쿠폰 보유 여부
    private Boolean isIssued;

    // Entity -> DTO 변환
    public CouponResponseDto(Coupon coupon) {
        CouponPolicy policy = coupon.getCouponPolicy();

        this.discountType = policy.getCouponPolicyDiscountType();

        if (this.discountType == CouponPolicyDiscountType.FIXED) {
            this.discountDescription = String.format("%,d원 할인", policy.getCouponDiscountValue());
            this.maxPrice = null;
        } else {
            this.discountDescription = String.format("%d%% 할인", policy.getCouponDiscountValue());
            this.maxPrice = policy.getMaxPrice();
        }

        this.couponId = coupon.getCouponId();
        this.couponName = policy.getCouponPolicyName();
        this.discountValue = policy.getCouponDiscountValue();
        this.minPrice = policy.getMinPrice();
        this.status = policy.getCouponPolicyStatus();

        if (policy.getFixedStartDate() != null && policy.getFixedEndDate() != null) {
            this.startDate = policy.getFixedStartDate();
            this.endDate = policy.getFixedEndDate();
            this.durationDays = null;
        } else {
            this.startDate = null;
            this.endDate = null;
            this.durationDays = policy.getDurationDays();
        }

        this.couponRemainingQuantity = coupon.getCouponRemainingQuantity();
    }

    public void setIsIssued() {
        this.isIssued = true;
    }
}
