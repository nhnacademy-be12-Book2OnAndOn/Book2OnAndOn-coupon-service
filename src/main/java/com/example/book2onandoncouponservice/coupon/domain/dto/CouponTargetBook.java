package com.example.book2onandoncouponservice.coupon.domain.dto;

import com.example.book2onandoncouponservice.couponpolicy.domain.entity.CouponDiscountType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CouponTargetBook {
    private Long couponId;
    private String couponName;
    private CouponDiscountType couponDiscountType;
    private String couponDiscountValue;
}
