package com.example.book2onandoncouponservice.coupon.domain.entity;

import com.example.book2onandoncouponservice.couponpolicy.domain.entity.CouponPolicy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
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
@Builder
@AllArgsConstructor
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    private Long couponId;

    @NotNull
    @Column(name = "coupon_name")
    private String couponName;

    @NotNull
    @Column(name = "user_id")
    private Long userId;

    @NotNull
    @Column(name = "order_id")
    private Long orderId;

    @NotNull
    @Column(name = "coupon_status")
    @Enumerated(EnumType.STRING)
    private CouponStatus couponStatus;

    @NotNull
    @Column(name = "coupon_issued_date")
    private LocalDate issuedDate;

    @Column(name = "coupon_used_date")
    private LocalDate usedDate;

    @NotNull
    @Column(name = "coupon_end_date")
    private LocalDate endDate;

    @ManyToOne
    @NotNull
    @JoinColumn(name = "coupon_policy_id")
    private CouponPolicy couponPolicy;

}
