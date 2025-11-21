package com.example.book2onandoncouponservice.dto.response;

import com.example.book2onandoncouponservice.entity.CouponPolicy;
import com.example.book2onandoncouponservice.entity.CouponPolicyDiscountType;
import com.example.book2onandoncouponservice.entity.CouponPolicyStatus;
import com.example.book2onandoncouponservice.entity.CouponPolicyType;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CouponPolicyResponseDto {

    private Long couponPolicyId;
    private String couponPolicyName;
    private CouponPolicyType couponPolicyType;
    private CouponPolicyDiscountType couponPolicyDiscountType;
    private Integer couponDiscountValue;
    private Integer minPrice;
    private Integer maxPrice;

    private Integer durationDays;
    private LocalDate fixedStartDate;
    private LocalDate fixedEndDate;

    private List<Long> targetBookIds;
    private List<Long> targetCategoryIds;
    private CouponPolicyStatus couponPolicyStatus;

    // Entity -> DTO 변환 메서드
    public CouponPolicyResponseDto(CouponPolicy entity, List<Long> targetBookIds, List<Long> targetCategoryIds) {
        this.couponPolicyId = entity.getCouponPolicyId();
        this.couponPolicyName = entity.getCouponPolicyName();
        this.couponPolicyType = entity.getCouponPolicyType();
        this.couponPolicyDiscountType = entity.getCouponPolicyDiscountType();
        this.couponDiscountValue = entity.getCouponDiscountValue();
        this.minPrice = entity.getMinPrice();
        this.maxPrice = entity.getMaxPrice();
        this.durationDays = entity.getDurationDays();
        this.fixedStartDate = entity.getFixedStartDate();
        this.fixedEndDate = entity.getFixedEndDate();
        this.targetBookIds = targetBookIds;
        this.targetCategoryIds = targetCategoryIds;
        this.couponPolicyStatus = entity.getCouponPolicyStatus();
    }
}