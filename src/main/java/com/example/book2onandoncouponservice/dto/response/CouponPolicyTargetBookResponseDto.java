package com.example.book2onandoncouponservice.dto.response;

import com.example.book2onandoncouponservice.entity.CouponPolicyTargetBook;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponPolicyTargetBookResponseDto {
    
    private Long policyTargetBookId;
    private Long couponPolicyId;
    private Long bookId;

    // Entity -> DTO 변환 메서드
    public CouponPolicyTargetBookResponseDto(CouponPolicyTargetBook entity) {
        this.policyTargetBookId = entity.getPolicyTargetBookId();
        this.couponPolicyId = entity.getCouponPolicy().getCouponPolicyId();
        this.bookId = entity.getBookId();
    }
}