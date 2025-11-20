package com.example.book2onandoncouponservice.dto.request;

import com.example.book2onandoncouponservice.entity.CouponDiscountType;
import com.example.book2onandoncouponservice.entity.CouponPolicyType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CouponPolicyUpdateRequestDto {

    @Size(max = 50, message = "쿠폰 정책 이름은 50자 이내여야 합니다.")
    private String couponPolicyName;

    private CouponPolicyType couponPolicyType;

    private CouponDiscountType couponDiscountType;

    @Min(value = 0, message = "할인 값은 0 이상이어야 합니다.")
    private Integer couponDiscountValue;

    @Min(value = 0, message = "최소 주문 금액은 0 이상이어야 합니다.")
    private Integer minPrice;

    @Min(value = 0, message = "최대 할인 금액은 0 이상이어야 합니다.")
    private Integer maxPrice;

    @Min(value = 1, message = "유효 기간은 1일 이상이어야 합니다.")
    private Integer durationDays;

    private LocalDate fixedStartDate;

    private LocalDate fixedEndDate;

    private List<Long> targetBookIds;

    private List<Long> targetCategoryIds;
}
