package com.example.book2onandoncouponservice.dto.request;

import com.example.book2onandoncouponservice.entity.CouponDiscountType;
import com.example.book2onandoncouponservice.entity.CouponPolicyType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CouponPolicyCreateRequestDto {

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    @NotNull(message = "쿠폰정책 타입은 필수입니다.")
    private CouponPolicyType type;

    @NotNull(message = "쿠폰할인 형식은 필수입니다.")
    private CouponDiscountType discountType;

    @NotNull(message = "할인율은 필수입니다.")
    @Min(value = 0, message = "할인율은 음수일 수 없습니다.")
    private Integer discountValue;

    @Min(value = 0, message = "최소구매금액은 음수일 수 없습니다.")
    private Integer minPrice;
    private Integer maxPrice;
    private Integer durationDays;
    private LocalDate fixedStartDate;
    private LocalDate fixedEndDate;
    private List<Long> targetBookIds;
    private List<Long> targetCategoryIds;
}