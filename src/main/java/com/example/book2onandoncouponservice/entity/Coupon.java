package com.example.book2onandoncouponservice.entity;

import com.example.book2onandoncouponservice.exception.CouponErrorCode;
import com.example.book2onandoncouponservice.exception.CouponIssueException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "coupon")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    private Long couponId;

    // 남은 재고 (null = 무제한)
    @Column(name = "coupon_remaining_quantity")
    private Integer couponRemainingQuantity;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_policy_id", nullable = false, unique = true)
    private CouponPolicy couponPolicy;

    // 생성자
    public Coupon(Integer remainingQuantity, CouponPolicy couponPolicy) {
        this.couponRemainingQuantity = remainingQuantity; // null이면 무제한
        this.couponPolicy = couponPolicy;
    }

    //쿠폰 수량 업데이트용
    public void update(Integer remainingQuantity) {
        if (remainingQuantity == null) {
            this.couponRemainingQuantity = null;
            return;
        }

        if (remainingQuantity < 0) {
            throw new IllegalStateException("쿠폰 수량은 음수일 수 없습니다.");
        }

        this.couponRemainingQuantity = remainingQuantity;
    }

    // 재고 차감
    public void decreaseStock() {
        if (couponRemainingQuantity != null) {
            if (couponRemainingQuantity <= 0) {
                throw new CouponIssueException(CouponErrorCode.COUPON_OUT_OF_STOCK);
            }
            couponRemainingQuantity--;
        }
    }
}
