package com.example.book2onandoncouponservice.repository;

import com.example.book2onandoncouponservice.entity.MemberCoupon;
import com.example.book2onandoncouponservice.entity.MemberCouponStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    Optional<MemberCoupon> findByOrderId(Long orderId);

    //BulkUpdate
    //만료된 쿠폰 전체 만료처리
    @Modifying(clearAutomatically = true) //영속성 컨테스트 초기화 필수
    @Query("UPDATE MemberCoupon mc " +
            "SET mc.memberCouponStatus = 'EXPIRED' " +
            "WHERE mc.memberCouponEndDate < :now " +
            "AND mc.memberCouponStatus = 'NOT_USED'")
    int bulkExpireCoupons(@Param("now") LocalDateTime now);
}
