package com.example.book2onandoncouponservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "CouponPolicy")
public class CouponPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_policy_id")
    private Long couponPolicyId;

    @NotNull
    @Column(name = "coupon_policy_name", length = 50)
    @Size(max = 50)
    private String couponPolicyName;

    @NotNull
    @Column(name = "coupon_policy_type", length = 100)
    @Enumerated(EnumType.STRING)
    private CouponPolicyType couponPolicyType;

    @NotNull
    @Column(name = "coupon_discount_type", length = 20)
    @Enumerated(EnumType.STRING)
    private CouponDiscountType couponDiscountType;

    @NotNull
    @Column(name = "coupon_discount_value", precision = 10, scale = 2)
    private BigDecimal couponDiscountValue;

    @NotNull
    @Column(name = "coupon_min_purchase_amount", precision = 12, scale = 2)
    private BigDecimal minPrice;

    @Column(name = "coupon_max_discount_amount", precision = 12, scale = 2)
    private BigDecimal maxPrice;

    @Column(name = "coupon_duration_days")
    private Integer durationDays;

    @Column(name = "coupon_fixed_start_date")
    private LocalDate fixedStartDate;

    @Column(name = "coupon_fixed_end_date")
    private LocalDate fixedEndDate;
}
