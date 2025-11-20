package com.example.book2onandoncouponservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
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

    // 남은 재고 (null = 무제한)
    @Column(name = "coupon_remaining_quantity")
    private Integer couponRemainingQuantity;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_policy_id", nullable = false)
    private CouponPolicy couponPolicy;

    // 생성자
    public Coupon(Integer remainingQuantity, CouponPolicy couponPolicy) {
        this.couponRemainingQuantity = remainingQuantity; // null이면 무제한
        this.couponPolicy = couponPolicy;
    }

    // 재고 차감
    public void decreaseStock() {
        if (couponRemainingQuantity != null) {
            if (couponRemainingQuantity <= 0) {
                throw new RuntimeException("쿠폰 재고가 모두 소진되었습니다.");
            }
            couponRemainingQuantity--;
        }
    }

    // 롤백
    public void increaseStock() {
        if (couponRemainingQuantity != null) {
            couponRemainingQuantity++;
        }
    }
}
