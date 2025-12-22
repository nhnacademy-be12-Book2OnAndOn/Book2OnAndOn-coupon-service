package com.example.book2onandoncouponservice.repository;

import com.example.book2onandoncouponservice.entity.MemberCoupon;
import com.example.book2onandoncouponservice.entity.MemberCouponStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberCouponRepository extends JpaRepository<MemberCoupon, Long> {

    boolean existsByUserIdAndCoupon_CouponId(Long userId, Long couponId);

    //특정 user가 보유한 쿠폰 목록 조회
    @Query("SELECT mc.coupon.couponId FROM MemberCoupon mc WHERE mc.userId = :userId")
    List<Long> findAllCouponIdsByUserId(@Param("userId") Long userId);

    @Query(value = """
            SELECT mc 
            FROM MemberCoupon mc
            JOIN FETCH mc.coupon c
            JOIN FETCH c.couponPolicy cp
            WHERE mc.userId = :userId
            AND (:status IS NULL OR mc.memberCouponStatus = :status)
            """,
            countQuery = """
                    SELECT count(mc) 
                    FROM MemberCoupon mc 
                    WHERE mc.userId = :userId
                    AND (:status IS NULL OR mc.memberCouponStatus = :status)
                    """)
    Page<MemberCoupon> findCouponsWithPolicy(
            @Param("userId") Long userId,
            @Param("status") MemberCouponStatus status,
            Pageable pageable
    );

    Optional<MemberCoupon> findByOrderNumber(String orderNumber);

    // 쿠폰 정책 id 리스트로 받아서 정책 id에 해당하는 쿠폰 조회
    @Query("SELECT mc FROM MemberCoupon mc " +
            "JOIN FETCH mc.coupon c " +
            "JOIN FETCH c.couponPolicy cp " + // fetch join으로 정책 정보까지 로딩
            "WHERE mc.userId = :userId " +
            "AND mc.memberCouponStatus = 'NOT_USED' " +
            "AND mc.memberCouponEndDate >= :now " +
            "AND cp.couponPolicyId IN :policyIds")
    // 정책 ID 목록에 포함되는지 확인
    List<MemberCoupon> findUsableCouponsByPolicyIds(@Param("userId") Long userId,
                                                    @Param("policyIds") List<Long> policyIds,
                                                    @Param("now") LocalDateTime now);

    //order에게 해당 쿠폰의 적용 book, category 반환용
    @Query("SELECT mc FROM MemberCoupon mc " +
            "JOIN FETCH mc.coupon c " +
            "JOIN FETCH c.couponPolicy cp " +
            "WHERE mc.memberCouponId = :memberCouponId")
    Optional<MemberCoupon> findByIdWithTargets(@Param("memberCouponId") Long memberCouponId);
}
