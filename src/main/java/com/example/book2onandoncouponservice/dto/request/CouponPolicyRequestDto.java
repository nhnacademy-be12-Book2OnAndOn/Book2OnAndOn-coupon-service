package com.example.book2onandoncouponservice.dto.request;

import com.example.book2onandoncouponservice.entity.CouponDiscountType;
import com.example.book2onandoncouponservice.entity.CouponPolicyType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CouponPolicyRequestDto {

    @NotBlank(message = "쿠폰 정책 이름은 필수입니다.")
    @Size(max = 50, message = "쿠폰 정책 이름은 50자 이내여야 합니다.")
    private String couponPolicyName;

    @NotNull(message = "쿠폰 정책 타입은 필수입니다.")
    private CouponPolicyType couponPolicyType;

    @NotNull(message = "할인 타입은 필수입니다.")
    private CouponDiscountType couponDiscountType;

    @NotNull(message = "할인 값은 필수입니다.")
    @Min(value = 0, message = "할인 값은 0 이상이어야 합니다.")
    private BigDecimal couponDiscountValue;

    @NotNull(message = "최소 주문 금액은 필수입니다.")
    @Min(value = 0, message = "최소 주문 금액은 0 이상이어야 합니다.")
    private BigDecimal minPrice;

    @Min(value = 0, message = "최대 할인 금액은 0 이상이어야 합니다.")
    private BigDecimal maxPrice;

    @Min(value = 1, message = "유효 기간은 1일 이상이어야 합니다.")
    private Integer durationDays;

    private LocalDate fixedStartDate;
    private LocalDate fixedEndDate;

    // 적용 대상 (도서 ID 리스트)
    private List<Long> targetBookIds;

    // 적용 대상 (카테고리 ID 리스트)
    private List<Long> targetCategoryIds;
}