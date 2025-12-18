package com.example.book2onandoncouponservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.book2onandoncouponservice.entity.CouponPolicy;
import com.example.book2onandoncouponservice.entity.CouponPolicyDiscountType;
import com.example.book2onandoncouponservice.entity.CouponPolicyStatus;
import com.example.book2onandoncouponservice.entity.CouponPolicyTargetBook;
import com.example.book2onandoncouponservice.entity.CouponPolicyTargetCategory;
import com.example.book2onandoncouponservice.entity.CouponPolicyType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class TargetRepositoryTest {

    @Autowired
    private CouponPolicyTargetBookRepository targetBookRepository;
    @Autowired
    private CouponPolicyTargetCategoryRepository targetCategoryRepository;
    @Autowired
    private TestEntityManager entityManager;

    private CouponPolicy createPolicy() {
        CouponPolicy p = new CouponPolicy();
        p.setCouponPolicyName("Policy");
        p.setCouponPolicyType(CouponPolicyType.BOOK);
        p.setCouponPolicyStatus(CouponPolicyStatus.ACTIVE);
        p.setCouponPolicyDiscountType(CouponPolicyDiscountType.FIXED);
        p.setCouponDiscountValue(1000);
        p.setMinPrice(0);
        return entityManager.persist(p);
    }

    @Test
    @DisplayName("타겟 도서 조회 및 일괄 삭제")
    void targetBook_Test() {
        // given
        CouponPolicy policy = createPolicy();
        Long policyId = policy.getCouponPolicyId();

        CouponPolicyTargetBook t1 = CouponPolicyTargetBook.builder().couponPolicy(policy).bookId(101L).build();
        CouponPolicyTargetBook t2 = CouponPolicyTargetBook.builder().couponPolicy(policy).bookId(102L).build();

        entityManager.persist(t1);
        entityManager.persist(t2);
        entityManager.flush();
        entityManager.clear();

        // when 1: 조회
        List<CouponPolicyTargetBook> found = targetBookRepository.findAllByCouponPolicy_CouponPolicyId(policyId);
        assertThat(found).hasSize(2);

        // when 2: 삭제
        targetBookRepository.deleteByCouponPolicy_CouponPolicyId(policyId);
        entityManager.flush();
        entityManager.clear();

        // then
        List<CouponPolicyTargetBook> afterDelete = targetBookRepository.findAllByCouponPolicy_CouponPolicyId(policyId);
        assertThat(afterDelete).isEmpty();
    }

    @Test
    @DisplayName("타겟 카테고리 조회 및 일괄 삭제")
    void targetCategory_Test() {
        // given
        CouponPolicy policy = createPolicy();
        Long policyId = policy.getCouponPolicyId();

        CouponPolicyTargetCategory c1 = CouponPolicyTargetCategory.builder().couponPolicy(policy).categoryId(10L)
                .build();
        entityManager.persist(c1);
        entityManager.flush();
        entityManager.clear();

        // when 1: 조회
        List<CouponPolicyTargetCategory> found = targetCategoryRepository.findAllByCouponPolicy_CouponPolicyId(
                policyId);
        assertThat(found).hasSize(1);

        // when 2: 삭제
        targetCategoryRepository.deleteByCouponPolicy_CouponPolicyId(policyId);
        entityManager.flush();
        entityManager.clear();

        // then
        List<CouponPolicyTargetCategory> afterDelete = targetCategoryRepository.findAllByCouponPolicy_CouponPolicyId(
                policyId);
        assertThat(afterDelete).isEmpty();
    }
}