package com.example.book2onandoncouponservice.repository;

import com.example.book2onandoncouponservice.entity.CouponPolicy;
import com.example.book2onandoncouponservice.entity.CouponPolicyDiscountType;
import com.example.book2onandoncouponservice.entity.CouponPolicyStatus;
import com.example.book2onandoncouponservice.entity.CouponPolicyType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponPolicyRepository extends JpaRepository<CouponPolicy, Long> {

    @Query("SELECT p FROM CouponPolicy p " +
            "WHERE (:type IS NULL OR p.couponPolicyType = :type) " +
            "AND (:discountType IS NULL OR p.couponPolicyDiscountType = :discountType) " +
            "AND (:status IS NULL OR p.couponPolicyStatus = :status)")
    Page<CouponPolicy> findAllByFilters(
            @Param("type") CouponPolicyType type,
            @Param("discountType") CouponPolicyDiscountType discountType,
            @Param("status") CouponPolicyStatus status,
            Pageable pageable
    );

    Optional<CouponPolicy> findByCouponPolicyType(CouponPolicyType couponPolicyType);
}
