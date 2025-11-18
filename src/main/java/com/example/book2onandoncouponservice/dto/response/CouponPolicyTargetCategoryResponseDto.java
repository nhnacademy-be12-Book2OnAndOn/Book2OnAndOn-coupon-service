package com.example.book2onandoncouponservice.dto.response;

import com.example.book2onandoncouponservice.entity.CouponPolicyTargetCategory;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CouponPolicyTargetCategoryResponseDto {

    private Long policyTargetCategoryId;
    private Long couponPolicyId;
    private Long categoryId;

    // Entity -> DTO 변환 메서드
    public CouponPolicyTargetCategoryResponseDto(CouponPolicyTargetCategory entity) {
        this.policyTargetCategoryId = entity.getPolicyTargetCategoryId();
        this.couponPolicyId = entity.getCouponPolicy().getCouponPolicyId();
        this.categoryId = entity.getCategoryId();
    }
}