package com.example.book2onandoncouponservice.dto.response;

import com.example.book2onandoncouponservice.entity.CouponDiscountType;
import com.example.book2onandoncouponservice.entity.CouponPolicy;
import com.example.book2onandoncouponservice.entity.CouponPolicyType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponPolicyResponseDto {

    private Long couponPolicyId;
    private String couponPolicyName;
    private CouponPolicyType couponPolicyType;
    private CouponDiscountType couponDiscountType;
    private BigDecimal couponDiscountValue;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    private Integer durationDays;
    private LocalDate fixedStartDate;
    private LocalDate fixedEndDate;

    private List<Long> targetBookIds;
    private List<Long> targetCategoryIds;

    // Entity -> DTO 변환 메서드
    public CouponPolicyResponseDto(CouponPolicy entity, List<Long> targetBookIds, List<Long> targetCategoryIds) {
        this.couponPolicyId = entity.getCouponPolicyId();
        this.couponPolicyName = entity.getCouponPolicyName();
        this.couponPolicyType = entity.getCouponPolicyType();
        this.couponDiscountType = entity.getCouponDiscountType();
        this.couponDiscountValue = entity.getCouponDiscountValue();
        this.minPrice = entity.getMinPrice();
        this.maxPrice = entity.getMaxPrice();
        this.durationDays = entity.getDurationDays();
        this.fixedStartDate = entity.getFixedStartDate();
        this.fixedEndDate = entity.getFixedEndDate();
        this.targetBookIds = targetBookIds;
        this.targetCategoryIds = targetCategoryIds;
    }
}