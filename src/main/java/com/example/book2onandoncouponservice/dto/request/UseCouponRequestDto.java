package com.example.book2onandoncouponservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UseCouponRequestDto {

    @NotNull(message = "주문번호는 필수입니다.")
    private String orderNumber;
}
