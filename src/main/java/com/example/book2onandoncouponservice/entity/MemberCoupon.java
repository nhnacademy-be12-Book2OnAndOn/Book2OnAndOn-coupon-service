package com.example.book2onandoncouponservice.entity;

import com.example.book2onandoncouponservice.exception.CouponErrorCode;
import com.example.book2onandoncouponservice.exception.CouponUseException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)

@Table(name = "member_coupon", indexes = {
        //유저의 쿠폰 목록 및 사용 가능 쿠폰 조회용 복합 인덱스
        @Index(name = "idx_member_coupon_user_status_end", columnList = "user_id, member_coupon_status, member_coupon_end_date"),

        //주문 취소/복구용 검색 인덱스
        @Index(name = "idx_member_coupon_order_number", columnList = "order_number")
},
        //쿠폰 중복 발급 방지를 위해 user_id와 coupon_id를 유니크 키로 설정
        uniqueConstraints = {

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
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "member_coupon_status", nullable = false)
    private MemberCouponStatus memberCouponStatus = MemberCouponStatus.NOT_USED;

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

    @Column(name = "order_number")
    private String orderNumber;

    public MemberCoupon(Long userId, Coupon coupon, LocalDateTime issuedDate, LocalDateTime endDate) {
        this.userId = userId;
        this.coupon = coupon;
        this.memberCouponStatus = MemberCouponStatus.NOT_USED;
        this.memberCouponIssuedDate = issuedDate;
        this.memberCouponEndDate = endDate;
    }

    public void use(String orderNumber) {

        if (this.memberCouponStatus == MemberCouponStatus.USED) {
            throw new CouponUseException(CouponErrorCode.COUPON_ALREADY_USED);
        }

        if (this.memberCouponStatus == MemberCouponStatus.EXPIRED ||
                LocalDateTime.now().isAfter(this.memberCouponEndDate)) {
            throw new CouponUseException(CouponErrorCode.COUPON_EXPIRED);
        }

        this.memberCouponStatus = MemberCouponStatus.USED;
        this.memberCouponUsedDate = LocalDateTime.now();
        this.orderNumber = orderNumber;
    }

    public void cancelUsage() {
        if (this.memberCouponStatus != MemberCouponStatus.USED) {
            throw new CouponUseException(CouponErrorCode.COUPON_NOT_USED);
        }

        this.memberCouponStatus = MemberCouponStatus.NOT_USED;
        this.memberCouponUsedDate = null;
        this.orderNumber = null;
    }

    public void expire() {
        if (this.memberCouponStatus == MemberCouponStatus.NOT_USED) {
            this.memberCouponStatus = MemberCouponStatus.EXPIRED;
        }
    }
}