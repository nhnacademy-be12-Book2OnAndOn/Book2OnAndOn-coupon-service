package com.example.book2onandoncouponservice.repository;

import com.example.book2onandoncouponservice.entity.CouponPolicyTargetCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponPolicyTargetCategoryRepository extends JpaRepository<CouponPolicyTargetCategory, Long> {

    //특정 정책에 연결된 타겟 데이터 전체 조회
    List<CouponPolicyTargetCategory> findAllByCouponPolicy_CouponPolicyId(Long policyId);

    //특정 정책의 대상 카테고리 일괄 삭제
    void deleteByCouponPolicy_CouponPolicyId(Long policyId);
    
    // 특정 정책이 특정 카테고리에 적용되는지 확인
    boolean existsByCouponPolicy_CouponPolicyIdAndCategoryId(Long policyId, Long categoryId);
}