package com.example.book2onandoncouponservice.repository;

import com.example.book2onandoncouponservice.entity.MemberCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberCouponRepository extends JpaRepository<MemberCoupon, Long> {

    boolean existsByUserIdAndCoupon_CouponId(Long userId, Long couponId);

}
