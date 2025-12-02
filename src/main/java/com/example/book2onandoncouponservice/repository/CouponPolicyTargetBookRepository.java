package com.example.book2onandoncouponservice.repository;

import com.example.book2onandoncouponservice.entity.CouponPolicyTargetBook;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponPolicyTargetBookRepository extends JpaRepository<CouponPolicyTargetBook, Long> {

    //특정 정책에 연결된 타겟 데이터 전체 조회
    List<CouponPolicyTargetBook> findAllByCouponPolicy_CouponPolicyId(Long policyId);

    // 특정 정책의 대상 도서 일괄 삭제
    void deleteByCouponPolicy_CouponPolicyId(Long policyId);

}