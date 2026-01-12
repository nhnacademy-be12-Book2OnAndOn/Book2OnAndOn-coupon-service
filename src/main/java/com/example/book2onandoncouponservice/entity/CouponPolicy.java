package com.example.book2onandoncouponservice.entity;

import com.example.book2onandoncouponservice.dto.request.CouponPolicyRequestDto;
import com.example.book2onandoncouponservice.dto.request.CouponPolicyUpdateRequestDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "coupon_policy",
        indexes = {
                //웰컴/생일/상품 적용 쿠폰 조회용 복합 인덱스
                @Index(name = "idx_policy_type_status", columnList = "coupon_policy_type, coupon_policy_status")
        })
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
    private CouponPolicyDiscountType couponPolicyDiscountType;

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
    @Enumerated(EnumType.STRING)
    private CouponPolicyStatus couponPolicyStatus;

    @OneToMany(mappedBy = "couponPolicy", fetch = FetchType.LAZY)
    @BatchSize(size = 100)
    private List<CouponPolicyTargetBook> couponPolicyTargetBooks = new ArrayList<>();

    @OneToMany(mappedBy = "couponPolicy", fetch = FetchType.LAZY)
    @BatchSize(size = 100)
    private List<CouponPolicyTargetCategory> couponPolicyTargetCategories = new ArrayList<>();

    public CouponPolicy(CouponPolicyRequestDto requestDto) {
        this.couponPolicyName = requestDto.getCouponPolicyName();
        this.couponPolicyType = requestDto.getCouponPolicyType();
        this.couponPolicyDiscountType = requestDto.getCouponPolicyDiscountType();
        this.couponDiscountValue = requestDto.getCouponDiscountValue();
        this.minPrice = requestDto.getMinPrice();
        this.maxPrice = requestDto.getMaxPrice();
        this.durationDays = requestDto.getDurationDays();
        this.fixedStartDate = requestDto.getFixedStartDate();
        this.fixedEndDate = requestDto.getFixedEndDate();
        this.couponPolicyStatus = CouponPolicyStatus.ACTIVE;
    }

    public void updatePolicy(CouponPolicyUpdateRequestDto requestDto) {

        // 기본 정보 수정 (Null이 아닐 때만 업데이트)
        if (requestDto.getCouponPolicyName() != null) {
            this.couponPolicyName = requestDto.getCouponPolicyName();
        }
        if (requestDto.getCouponPolicyType() != null) {
            this.couponPolicyType = requestDto.getCouponPolicyType();
        }
        if (requestDto.getCouponPolicyDiscountType() != null) {
            this.couponPolicyDiscountType = requestDto.getCouponPolicyDiscountType();
        }
        if (requestDto.getCouponDiscountValue() != null) {
            this.couponDiscountValue = requestDto.getCouponDiscountValue();
        }
        if (requestDto.getMinPrice() != null) {
            this.minPrice = requestDto.getMinPrice();
        }

        // 최대 할인 금액 수정 (삭제 우선)
        if (Boolean.TRUE.equals(requestDto.getRemoveMaxPrice())) {
            this.maxPrice = null;
        } else if (requestDto.getMaxPrice() != null) {
            this.maxPrice = requestDto.getMaxPrice();
        }

        // 유효 기간 (Duration) 수정
        // 삭제 요청이 있으면 null 처리, 아니면 값 업데이트
        if (Boolean.TRUE.equals(requestDto.getRemoveDurationDays())) {
            this.durationDays = null;
        } else if (requestDto.getDurationDays() != null) {
            this.durationDays = requestDto.getDurationDays();
        }

        // 고정 기간 (Fixed Date) 수정
        // 시작일과 종료일은 보통 같이 움직이므로 하나의 블록으로 처리하는 것이 안전함
        if (Boolean.TRUE.equals(requestDto.getRemoveFixedDate())) {
            this.fixedStartDate = null;
            this.fixedEndDate = null;
        } else {
            if (requestDto.getFixedStartDate() != null) {
                this.fixedStartDate = requestDto.getFixedStartDate();
            }
            if (requestDto.getFixedEndDate() != null) {
                this.fixedEndDate = requestDto.getFixedEndDate();
            }
        }
    }

    public void deActive() {
        this.couponPolicyStatus = CouponPolicyStatus.DEACTIVE;
    }

    public boolean isIssuable() {
        return this.couponPolicyStatus.equals(CouponPolicyStatus.ACTIVE);
    }

}
