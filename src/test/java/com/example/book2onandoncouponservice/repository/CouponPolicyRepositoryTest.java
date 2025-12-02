package com.example.book2onandoncouponservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.book2onandoncouponservice.entity.CouponPolicy;
import com.example.book2onandoncouponservice.entity.CouponPolicyDiscountType;
import com.example.book2onandoncouponservice.entity.CouponPolicyStatus;
import com.example.book2onandoncouponservice.entity.CouponPolicyType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class CouponPolicyRepositoryTest {

    @Autowired
    private CouponPolicyRepository couponPolicyRepository;

    @Autowired
    private TestEntityManager entityManager;

    // Helper: 더미 정책 생성 및 저장
    private CouponPolicy savePolicy(String name, CouponPolicyType type,
                                    CouponPolicyDiscountType discountType,
                                    CouponPolicyStatus status) {
        CouponPolicy policy = new CouponPolicy();
        // (Entity에 @Builder나 Setter가 있다고 가정)
        // 실제로는 Entity 필드 접근 방식에 맞춰 수정해주세요 (ReflectionTestUtils 사용 가능)
        // 예시:
        policy.setCouponPolicyName(name);
        policy.setCouponPolicyType(type);
        policy.setCouponPolicyDiscountType(discountType);
        policy.setCouponPolicyStatus(status);
        policy.setCouponDiscountValue(1000);
        policy.setMinPrice(10000);

        return entityManager.persist(policy);
    }

    @Test
    @DisplayName("동적 쿼리 테스트 - 모든 조건 null일 때 전체 조회")
    void findAllByFilters_All() {
        // given
        savePolicy("P1", CouponPolicyType.WELCOME, CouponPolicyDiscountType.FIXED, CouponPolicyStatus.ACTIVE);
        savePolicy("P2", CouponPolicyType.BOOK, CouponPolicyDiscountType.PERCENT, CouponPolicyStatus.DEACTIVE);

        // when
        Page<CouponPolicy> result = couponPolicyRepository.findAllByFilters(null, null, null, PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("동적 쿼리 테스트 - 상태(Status) 필터링")
    void findAllByFilters_Status() {
        // given
        savePolicy("Active Policy", CouponPolicyType.WELCOME, CouponPolicyDiscountType.FIXED,
                CouponPolicyStatus.ACTIVE);
        savePolicy("Deactive Policy", CouponPolicyType.WELCOME, CouponPolicyDiscountType.FIXED,
                CouponPolicyStatus.DEACTIVE);

        // when
        Page<CouponPolicy> result = couponPolicyRepository.findAllByFilters(null, null, CouponPolicyStatus.ACTIVE,
                PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCouponPolicyStatus()).isEqualTo(CouponPolicyStatus.ACTIVE);
    }

    @Test
    @DisplayName("동적 쿼리 테스트 - 타입(Type) 필터링")
    void findAllByFilters_Type() {
        // given
        savePolicy("Welcome", CouponPolicyType.WELCOME, CouponPolicyDiscountType.FIXED, CouponPolicyStatus.ACTIVE);
        savePolicy("Book", CouponPolicyType.BOOK, CouponPolicyDiscountType.FIXED, CouponPolicyStatus.ACTIVE);

        // when
        Page<CouponPolicy> result = couponPolicyRepository.findAllByFilters(CouponPolicyType.BOOK, null, null,
                PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCouponPolicyType()).isEqualTo(CouponPolicyType.BOOK);
    }

    @Test
    @DisplayName("동적 쿼리 테스트 - 복합 조건 (Type + Status)")
    void findAllByFilters_Complex() {
        // given
        savePolicy("Target", CouponPolicyType.BOOK, CouponPolicyDiscountType.FIXED, CouponPolicyStatus.ACTIVE);
        savePolicy("Wrong Type", CouponPolicyType.WELCOME, CouponPolicyDiscountType.FIXED, CouponPolicyStatus.ACTIVE);
        savePolicy("Wrong Status", CouponPolicyType.BOOK, CouponPolicyDiscountType.FIXED, CouponPolicyStatus.DEACTIVE);

        // when
        Page<CouponPolicy> result = couponPolicyRepository.findAllByFilters(
                CouponPolicyType.BOOK,
                null,
                CouponPolicyStatus.ACTIVE,
                PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCouponPolicyName()).isEqualTo("Target");
    }
}