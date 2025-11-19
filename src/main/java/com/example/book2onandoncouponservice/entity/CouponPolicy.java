package com.example.book2onandoncouponservice.entity;

import com.example.book2onandoncouponservice.dto.request.CouponPolicyCreateRequestDto;
import com.example.book2onandoncouponservice.dto.request.CouponPolicyUpdateRequestDto;
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
import java.time.LocalDate;
import lombok.AllArgsConstructor;
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
    private Integer couponDiscountValue;

    @NotNull
    @Column(name = "coupon_min_purchase_amount", precision = 12, scale = 2)
    private Integer minPrice;

    @Column(name = "coupon_max_discount_amount", precision = 12, scale = 2)
    private Integer maxPrice;

    @Column(name = "coupon_duration_days")
    private Integer durationDays;

    @Column(name = "coupon_fixed_start_date")
    private LocalDate fixedStartDate;

    @Column(name = "coupon_fixed_end_date")
    private LocalDate fixedEndDate;

    @Column(name = "coupon_policy_status")
    private CouponPolicyStatus couponPolicyStatus;

    public CouponPolicy(CouponPolicyCreateRequestDto requestDto) {
        this.couponPolicyName = requestDto.getName();
        this.couponPolicyType = requestDto.getType();
        this.couponDiscountType = requestDto.getDiscountType();
        this.couponDiscountValue = requestDto.getDiscountValue();
        this.minPrice = requestDto.getMinPrice();
        this.maxPrice = requestDto.getMaxPrice();
        this.durationDays = requestDto.getDurationDays();
        this.fixedStartDate = requestDto.getFixedStartDate();
        this.fixedEndDate = requestDto.getFixedEndDate();
        this.couponPolicyStatus = CouponPolicyStatus.ACTIVE;
    }

    public void updatePolicy(CouponPolicyUpdateRequestDto requestDto) {

        if (requestDto.getCouponPolicyName() != null) {
            this.couponPolicyName = requestDto.getCouponPolicyName();
        }

        if (requestDto.getCouponPolicyType() != null) {
            this.couponPolicyType = requestDto.getCouponPolicyType();
        }

        if (requestDto.getCouponDiscountType() != null) {
            this.couponDiscountType = requestDto.getCouponDiscountType();
        }

        if (requestDto.getCouponDiscountValue() != null) {
            this.couponDiscountValue = requestDto.getCouponDiscountValue();
        }

        if (requestDto.getMinPrice() != null) {
            this.minPrice = requestDto.getMinPrice();
        }

        if (requestDto.getMaxPrice() != null) {
            this.maxPrice = requestDto.getMaxPrice();
        }

        if (requestDto.getDurationDays() != null) {
            this.durationDays = requestDto.getDurationDays();
        }

        if (requestDto.getFixedStartDate() != null) {
            this.fixedStartDate = requestDto.getFixedStartDate();
        }

        if (requestDto.getFixedEndDate() != null) {
            this.fixedEndDate = requestDto.getFixedEndDate();
        }
    }

    public void deActive() {
        this.couponPolicyStatus = CouponPolicyStatus.DEACTIVE;
    }

}
