package com.example.book2onandoncouponservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
//쿠폰 중복 발급 방지를 위해 user_id와 coupon_id를 유니크 키로 설정
@Table(name = "MemberCoupon", uniqueConstraints = {
        @UniqueConstraint(
                name = "MEMBER_COUPON_UNIQUE",
                columnNames = {"user_id", "coupon_id"}
        )
})
public class MemberCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_coupon_id")
    private Long memberCouponId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    // 쿠폰 상태
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "member_coupon_status", nullable = false)
    private MemberCouponStatus memberCouponStatus;

    //쿠폰 발행일
    @NotNull
    @Column(name = "member_coupon_issued_date", nullable = false)
    private LocalDateTime memberCouponIssuedDate;

    //쿠폰 만료일
    @NotNull
    @Column(name = "member_coupon_end_date", nullable = false)
    private LocalDateTime memberCouponEndDate;

    // 쿠폰 사용일
    @Column(name = "member_coupon_used_date")
    private LocalDateTime memberCouponUsedDate;


    public MemberCoupon(Long userId, Coupon coupon, LocalDateTime issuedDate, LocalDateTime endDate) {
        this.userId = userId;
        this.coupon = coupon;
        this.memberCouponStatus = MemberCouponStatus.NOT_USED;
        this.memberCouponIssuedDate = issuedDate;
        this.memberCouponEndDate = endDate;
    }

    public void use() {
        if (this.memberCouponStatus != MemberCouponStatus.NOT_USED) {
            throw new IllegalStateException("이미 사용했거나 만료된 쿠폰입니다.");
        }
        if (LocalDateTime.now().isAfter(this.memberCouponEndDate)) {
            throw new IllegalStateException("유효 기간이 지난 쿠폰입니다.");
        }

        this.memberCouponStatus = MemberCouponStatus.USED;
        this.memberCouponUsedDate = LocalDateTime.now();
    }

    public void cancelUsage() {
        if (this.memberCouponStatus != MemberCouponStatus.USED) {
            throw new IllegalStateException("사용된 쿠폰만 취소할 수 있습니다.");
        }

        this.memberCouponStatus = MemberCouponStatus.NOT_USED;
        this.memberCouponUsedDate = null;
    }

    public void expired() {
        if (this.memberCouponStatus == MemberCouponStatus.EXPIRED) {
            throw new IllegalStateException("이미 만료된 쿠폰입니다.");
        }

        if (this.memberCouponStatus == MemberCouponStatus.USED) {
            throw new IllegalStateException("이미 사용한 쿠폰입니다.");
        }

        this.memberCouponStatus = MemberCouponStatus.EXPIRED;
    }
}