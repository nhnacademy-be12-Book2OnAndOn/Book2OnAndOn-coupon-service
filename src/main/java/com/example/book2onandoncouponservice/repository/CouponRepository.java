package com.example.book2onandoncouponservice.repository;


import com.example.book2onandoncouponservice.entity.Coupon;
import com.example.book2onandoncouponservice.entity.CouponStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    //사용자가 쿠폰 조회 시 할인율을 보여줘야 함
    @EntityGraph(attributePaths = {"couponPolicy"})
    List<Coupon> findByUserId(Long userId);

    //Pageable 고려
    @EntityGraph(attributePaths = {"couponPolicy"})
    Page<Coupon> findByUserId(Long userId, Pageable pageable);

    // 상태별 조회
    @EntityGraph(attributePaths = {"couponPolicy"})
    List<Coupon> findByUserIdAndCouponStatus(Long userId, CouponStatus couponStatus);

    // 중복 발급 체크
    boolean existsByUserIdAndCouponPolicy_CouponPolicyId(Long userId, Long policyId);

    // 주문 취소 시 복구용
    Optional<Coupon> findByOrderId(Long orderId);

    // 만료 처리 배치용 (Batch)
    List<Coupon> findByCouponStatusAndEndDateBefore(CouponStatus status, LocalDateTime now);

}
