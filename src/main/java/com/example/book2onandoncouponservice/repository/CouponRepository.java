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

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    //관리자용 조회
    @EntityGraph(attributePaths = {"couponPolicy"})
    Page<Coupon> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"couponPolicy"})
    Optional<Coupon> findById(Long couponId);

    //사용자용 (발급 가능 여부 체크)
    @EntityGraph(attributePaths = {"couponPolicy"})
    Page<Coupon> findByCouponPolicy_CouponPolicyStatusAndCouponIssueCountLessThanAndCouponPolicy_FixedEndDateGreaterThanEqual(
            CouponPolicyStatus status, Integer totalQuantityThreshold, LocalDate fixedEndDate, Pageable pageable
    );

    //재고 차감 (동시성 제어용)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"couponPolicy"})
    Optional<Coupon> findByIdForUpdate(Long couponId);
}