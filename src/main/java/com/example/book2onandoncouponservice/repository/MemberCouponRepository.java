package com.example.book2onandoncouponservice.repository;

import com.example.book2onandoncouponservice.entity.MemberCoupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MemberCouponRepository extends JpaRepository<MemberCoupon, Long> {

    boolean existsByUserIdAndCoupon_CouponId(Long userId, Long couponId);


    @Query("""
                select mc from MemberCoupon mc
                join fetch mc.coupon c
                join fetch c.couponPolicy cp
                where mc.userId = :userId
            """)
    Page<MemberCoupon> findCouponsWithPolicy(Long userId, Pageable pageable);
}
