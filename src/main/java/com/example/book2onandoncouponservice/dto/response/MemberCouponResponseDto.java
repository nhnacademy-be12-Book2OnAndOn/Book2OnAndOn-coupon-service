package com.example.book2onandoncouponservice.dto.response;

import com.example.book2onandoncouponservice.entity.Coupon;
import com.example.book2onandoncouponservice.entity.CouponPolicy;
import com.example.book2onandoncouponservice.entity.CouponPolicyDiscountType;
import com.example.book2onandoncouponservice.entity.MemberCoupon;
import com.example.book2onandoncouponservice.entity.MemberCouponStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MemberCouponResponseDto {

    private Long memberCouponId;

    private String couponName;                // CouponPolicy
    private Integer minPrice;                 // CouponPolicy
    private Integer maxPrice;                 // CouponPolicy
    private Integer discountValue;            // CouponPolicy
    private CouponPolicyDiscountType discountType;
    private MemberCouponStatus memberCouponStatus;   // MemberCoupon
    private LocalDateTime memberCouponEndDate;       // MemberCoupon
    private LocalDateTime memberCouponUseDate;       // MemberCoupon
    private String discountDescription;


    public MemberCouponResponseDto(MemberCoupon memberCoupon) {
        this.memberCouponId = memberCoupon.getMemberCouponId();
        this.memberCouponStatus = memberCoupon.getMemberCouponStatus();
        this.memberCouponEndDate = memberCoupon.getMemberCouponEndDate();
        this.memberCouponUseDate = memberCoupon.getMemberCouponUsedDate();

        // CouponPolicy 접근
        Coupon coupon = memberCoupon.getCoupon();
        CouponPolicy policy = coupon.getCouponPolicy();

        String discountDescription;
        if (policy.getCouponPolicyDiscountType() == CouponPolicyDiscountType.FIXED) {
            discountDescription = String.format("%,d원 할인", policy.getCouponDiscountValue());
        } else {
            discountDescription = String.format("%d%% 할인", policy.getCouponDiscountValue());
        }

        this.couponName = policy.getCouponPolicyName();
        this.minPrice = policy.getMinPrice();
        this.maxPrice = policy.getMaxPrice();
        this.discountValue = policy.getCouponDiscountValue();
        this.discountType = policy.getCouponPolicyDiscountType();
        this.discountDescription = discountDescription;
    }
}

