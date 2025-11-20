package com.example.book2onandoncouponservice.dto.response;

import com.example.book2onandoncouponservice.entity.Coupon;
import com.example.book2onandoncouponservice.entity.CouponPolicy;
import com.example.book2onandoncouponservice.entity.CouponPolicyDiscountType;
import com.example.book2onandoncouponservice.entity.CouponPolicyStatus;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CouponResponseDto {
    private Long couponId;
    private String couponName; // 정책 이름 또는 개별 쿠폰 이름
    private String discountDescription; // 예: "10% 할인" 또는 "3000원 할인"
    private Integer discountValue;
    private CouponPolicyDiscountType discountType;
    private Integer minPrice; // 사용 조건
    private Integer maxPrice; // 최대 할인 한도
    private Integer durationDays; // 상대유효기간
    private LocalDate startDate; //시작일
    private LocalDate endDate; // 만료일
    private CouponPolicyStatus status; // 사용 가능 여부
    private Integer couponTotalQuantity;
    private Integer couponIssueCount;

    // Entity -> DTO 변환 메서드
    public CouponResponseDto(Coupon coupon) {
        CouponPolicy couponPolicy = coupon.getCouponPolicy();

        String discountDescription;
        if (couponPolicy.getCouponPolicyDiscountType().equals(CouponPolicyDiscountType.FIXED)) {
            discountDescription = String.format("%,d원 할인", couponPolicy.getCouponDiscountValue().intValue());
        } else {
            discountDescription = String.format("%d%% 할인", couponPolicy.getCouponDiscountValue());
        }

        this.couponId = coupon.getCouponId();
        this.couponName = couponPolicy.getCouponPolicyName();
        this.discountDescription = discountDescription; // 계산된 설명 할당
        this.discountValue = couponPolicy.getCouponDiscountValue();
        this.discountType = couponPolicy.getCouponPolicyDiscountType();
        this.minPrice = couponPolicy.getMinPrice();
        this.maxPrice = couponPolicy.getMaxPrice();
        this.status = couponPolicy.getCouponPolicyStatus();

        if (couponPolicy.getCouponPolicyDiscountType().equals(CouponPolicyDiscountType.FIXED)) {
            this.startDate = couponPolicy.getFixedStartDate();
            this.endDate = couponPolicy.getFixedEndDate();
        }
    }
}