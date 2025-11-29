package com.example.book2onandoncouponservice.repository;

import com.example.book2onandoncouponservice.entity.MemberCoupon;
import com.example.book2onandoncouponservice.entity.MemberCouponStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberCouponRepository extends JpaRepository<MemberCoupon, Long> {

    boolean existsByUserIdAndCoupon_CouponId(Long userId, Long couponId);


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
}
