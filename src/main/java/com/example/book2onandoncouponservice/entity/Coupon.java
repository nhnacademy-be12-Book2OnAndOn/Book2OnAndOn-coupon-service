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
import jakarta.validation.constraints.NotNull;
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

    @Column(name = "coupon_total_quantity", nullable = false)
    private Integer couponTotalQuantity;

    @NotNull
    @Column(name = "coupon_issue_count", nullable = false)
    private Integer couponIssueCount = 0;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_policy_id", nullable = false)
    private CouponPolicy couponPolicy;

    //쿠폰 생성시 필요한 생성자 추가
    public Coupon(Integer couponTotalQuantity, CouponPolicy couponPolicy) {
        this.couponTotalQuantity = couponTotalQuantity;
        this.couponPolicy = couponPolicy;
        this.couponIssueCount = 0;
    }

    public void issue() {
        if (this.couponTotalQuantity != null && this.couponIssueCount >= this.couponTotalQuantity) {
            throw new RuntimeException("쿠폰이 모두 소진되었습니다.");
        }
        
        this.couponIssueCount++;
    }

    public void rollback() {
        if (this.couponIssueCount <= 0) {
            throw new IllegalStateException("쿠폰 발급 오류");
        }
        this.couponIssueCount--;
    }


}
