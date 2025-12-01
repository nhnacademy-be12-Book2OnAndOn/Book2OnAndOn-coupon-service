package com.example.book2onandoncouponservice.repository;

import com.example.book2onandoncouponservice.entity.Coupon;
import com.example.book2onandoncouponservice.entity.CouponPolicyStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
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
    @Query("SELECT c FROM Coupon c " +
            "JOIN FETCH c.couponPolicy p " + // N+1 문제 방지
            "WHERE (:status IS NULL OR p.couponPolicyStatus = :status)")
    Page<Coupon> findAllByPolicyStatus(@Param("status") CouponPolicyStatus status, Pageable pageable);

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

    @Query("SELECT DISTINCT c FROM Coupon c " +
            "JOIN FETCH c.couponPolicy p " +  // 쿠폰과 정책을 한번에 로딩 (N+1 방지)
            "LEFT JOIN CouponPolicyTargetBook b ON b.couponPolicy = p " +
            "LEFT JOIN CouponPolicyTargetCategory cat ON cat.couponPolicy = p " +
            "WHERE p.couponPolicyStatus = 'ACTIVE' " +
            "AND (" +
            "   (p.couponPolicyType = 'BOOK' AND b.bookId = :bookId) " +
            "   OR " +
            "   (p.couponPolicyType = 'CATEGORY' AND cat.categoryId IN :categoryIds) " +
            "   OR " +
            "   (p.couponPolicyType = 'WELCOME' OR p.couponPolicyType = 'GENERAL') " + // 전체 대상
            ")")
    List<Coupon> findAppliableCoupons(@Param("bookId") Long bookId,
                                      @Param("categoryIds") List<Long> categoryIds);
}