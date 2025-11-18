package com.example.book2onandoncouponservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Coupon")
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    private Long couponId;

    @NotNull
    @Size(max = 100)
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
    private LocalDateTime issuedDate;

    @Column(name = "coupon_used_date")
    private LocalDateTime usedDate;

    @NotNull
    @Column(name = "coupon_end_date")
    private LocalDateTime endDate;

    @ManyToOne
    @NotNull
    @JoinColumn(name = "coupon_policy_id")
    private CouponPolicy couponPolicy;

    //쿠폰 생성시 필요한 생성자 추가
    public Coupon(String couponName, Long userId, CouponPolicy couponPolicy,
                  LocalDateTime issuedDate, LocalDateTime endDate) {
        this.couponName = couponName;
        this.userId = userId;
        this.couponPolicy = couponPolicy;
        this.issuedDate = issuedDate;
        this.endDate = endDate;
        this.couponStatus = CouponStatus.NOT_USED; // 쿠폰 생성 시 상태 NOT_USED
    }

    //더티체킹 쿠폰사용 메서드
    public void use(Long orderId) {
        // 사용한 쿠폰인지 검증
        if (this.couponStatus != CouponStatus.NOT_USED) {
            throw new IllegalStateException("이미 사용했거나 취소된 쿠폰입니다.");
        }

        // 유효 기간이 지났는지 검증
        if (LocalDateTime.now().isAfter(this.endDate)) {
            throw new IllegalStateException("유효 기간이 지난 쿠폰입니다.");
        }

        this.couponStatus = CouponStatus.USED;
        this.orderId = orderId;
        this.usedDate = LocalDateTime.now();
    }

    //더티체킹 쿠폰사용 취소 메서드
    public void cancelUsage() {
        if (this.couponStatus != CouponStatus.USED) {
            throw new IllegalArgumentException("사용한 쿠폰이 아닙니다.");
        }

        this.couponStatus = CouponStatus.NOT_USED;
        this.orderId = null;
        this.usedDate = null;
    }

}
