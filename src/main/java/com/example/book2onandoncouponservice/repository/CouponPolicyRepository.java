package com.example.book2onandoncouponservice.repository;

import com.example.book2onandoncouponservice.entity.CouponPolicy;
import com.example.book2onandoncouponservice.entity.CouponPolicyType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponPolicyRepository extends JpaRepository<CouponPolicy, Long> {
    List<CouponPolicy> findAllBy();

    Page<CouponPolicy> findAll(Pageable pageable);

    //정책 중복 방지
    boolean existsByCouponPolicyName(String couponPolicyName);

    Optional<CouponPolicy> findByCouponPolicyType(CouponPolicyType couponPolicyType);

    //서비스 작성하면서 필요한 메서드 추가 예정
}
