package com.example.book2onandoncouponservice.repository;

import com.example.book2onandoncouponservice.entity.Coupon;
import com.example.book2onandoncouponservice.entity.CouponPolicyStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    //관리자용 조회
    @EntityGraph(attributePaths = {"couponPolicy"})
    Page<Coupon> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"couponPolicy"})
    Optional<Coupon> findById(Long couponId);

    @Query("""
            SELECT c
            FROM Coupon c
            WHERE c.couponPolicy.couponPolicyStatus = :status
              AND (c.couponRemainingQuantity IS NULL OR c.couponRemainingQuantity > 0)
              AND (c.couponPolicy.fixedEndDate IS NULL OR c.couponPolicy.fixedEndDate >= :today)
            """)
    @EntityGraph(attributePaths = {"couponPolicy"})
    Page<Coupon> findAvailableCoupons(
            @Param("status") CouponPolicyStatus status,
            @Param("today") LocalDate today,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.couponId = :couponId")
    Optional<Coupon> findByIdForUpdate(@Param("couponId") Long couponId);

    //쿠폰정책 ID로 해당 쿠폰을 찾는 메서드
    Optional<Coupon> findByCouponPolicy_CouponPolicyId(Long couponPolicyId);
}