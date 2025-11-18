package com.example.book2onandoncouponservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CouponIssueRequestDto {
    @NotNull(message = "유저ID는 필수입니다.")
    private Long userId;
    
    @NotNull(message = "쿠폰정책ID는 필수입니다.")
    private Long couponPolicyId;
}