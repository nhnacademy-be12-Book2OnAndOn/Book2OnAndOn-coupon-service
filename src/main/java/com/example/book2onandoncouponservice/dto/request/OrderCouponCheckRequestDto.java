package com.example.book2onandoncouponservice.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderCouponCheckRequestDto {

    @NotNull(message = "책ID는 필수입니다.")
    List<Long> bookIds;
    @NotNull(message = "회원ID는 필수입니다.")
    List<Long> categoryIds;
}