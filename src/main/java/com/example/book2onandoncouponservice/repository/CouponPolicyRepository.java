package com.example.book2onandoncouponservice.repository;

import com.example.book2onandoncouponservice.entity.CouponPolicy;
import com.example.book2onandoncouponservice.entity.CouponPolicyDiscountType;
import com.example.book2onandoncouponservice.entity.CouponPolicyStatus;
import com.example.book2onandoncouponservice.entity.CouponPolicyType;
import java.util.List;
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

    @Query("SELECT cp FROM CouponPolicy cp " +
            "WHERE cp.couponPolicyType = :type " +
            "AND cp.couponPolicyStatus = 'ACTIVE'")
    Optional<CouponPolicy> findActivePolicyByType(@Param("type") CouponPolicyType type);


    // 주문에게서 받은 bookids, categoryIds를 포함하는 쿠폰 정책 조회
    @Query("SELECT DISTINCT p.couponPolicyId FROM CouponPolicy p " +
            "LEFT JOIN p.couponPolicyTargetBooks tb " +
            "LEFT JOIN p.couponPolicyTargetCategories tc " +
            "WHERE " +
            "   (tb.bookId IN :bookIds) OR " +           // 도서 일치
            "   (tc.categoryId IN :categoryIds) OR " +   // 카테고리 일치
            "   (SIZE(p.couponPolicyTargetBooks) = 0 AND SIZE(p.couponPolicyTargetCategories) = 0)")
    // 제약 조건이 없는 쿠 폰
    List<Long> findApplicablePolicyIds(@Param("bookIds") List<Long> bookIds,
                                       @Param("categoryIds") List<Long> categoryIds);
}
