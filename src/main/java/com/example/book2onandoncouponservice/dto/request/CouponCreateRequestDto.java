package com.example.book2onandoncouponservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CouponCreateRequestDto {


    @Min(value = 1, message = "쿠폰 수량은 최소 1개 이상이어야 합니다.")
    private Integer coupon_total_quantity;

    @NotNull(message = "쿠폰정책 ID는 필수입니다.")
    private Long couponPolicyId;
}