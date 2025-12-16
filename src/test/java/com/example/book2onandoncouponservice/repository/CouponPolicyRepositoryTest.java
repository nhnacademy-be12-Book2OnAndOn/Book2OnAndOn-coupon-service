package com.example.book2onandoncouponservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.book2onandoncouponservice.entity.CouponPolicy;
import com.example.book2onandoncouponservice.entity.CouponPolicyDiscountType;
import com.example.book2onandoncouponservice.entity.CouponPolicyStatus;
import com.example.book2onandoncouponservice.entity.CouponPolicyTargetBook;
import com.example.book2onandoncouponservice.entity.CouponPolicyTargetCategory;
import com.example.book2onandoncouponservice.entity.CouponPolicyType;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = {"dooray.url=http://localhost"})
class CouponPolicyRepositoryTest {

    @Autowired
    private CouponPolicyRepository couponPolicyRepository;

    @Autowired
    private TestEntityManager entityManager;

    private CouponPolicy savePolicy(String name, CouponPolicyType type,
                                    CouponPolicyDiscountType discountType,
                                    CouponPolicyStatus status) {
        CouponPolicy policy = CouponPolicy.builder()
                .couponPolicyName(name)
                .couponPolicyType(type)
                .couponPolicyDiscountType(discountType)
                .couponPolicyStatus(status)
                .couponDiscountValue(1000)  // 테스트용 기본값
                .minPrice(10000)            // 테스트용 기본값
                .build();

        return entityManager.persist(policy);
    }

    // 타겟 도서 추가 헬퍼
    private void addTargetBook(CouponPolicy policy, Long bookId) {
        CouponPolicyTargetBook target = CouponPolicyTargetBook.builder()
                .couponPolicy(policy)
                .bookId(bookId)
                .build();
        entityManager.persist(target);
    }

    // 타겟 카테고리 추가 헬퍼
    private void addTargetCategory(CouponPolicy policy, Long categoryId) {
        CouponPolicyTargetCategory target = CouponPolicyTargetCategory.builder()
                .couponPolicy(policy)
                .categoryId(categoryId)
                .build();
        entityManager.persist(target);
    }

    // --- Tests ---

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

    @Test
    @DisplayName("주문 상품(도서, 카테고리)에 적용 가능한 정책 ID 조회")
    void findApplicablePolicyIds_Test() {
        // given
        // 1. 특정 도서(100L) 대상 정책
        CouponPolicy bookPolicy = savePolicy("Book Target", CouponPolicyType.BOOK, CouponPolicyDiscountType.FIXED,
                CouponPolicyStatus.ACTIVE);
        addTargetBook(bookPolicy, 100L);

        // 2. 특정 카테고리(20L) 대상 정책
        CouponPolicy categoryPolicy = savePolicy("Category Target", CouponPolicyType.BOOK,
                CouponPolicyDiscountType.FIXED, CouponPolicyStatus.ACTIVE);
        addTargetCategory(categoryPolicy, 20L);

        // 3. 전체 대상 정책 (타겟 없음)
        CouponPolicy allPolicy = savePolicy("All Target", CouponPolicyType.WELCOME, CouponPolicyDiscountType.FIXED,
                CouponPolicyStatus.ACTIVE);

        // 4. 해당 사항 없는 정책 (다른 도서 999L)
        CouponPolicy otherBookPolicy = savePolicy("Other Book", CouponPolicyType.BOOK, CouponPolicyDiscountType.FIXED,
                CouponPolicyStatus.ACTIVE);
        addTargetBook(otherBookPolicy, 999L);

        entityManager.flush();
        entityManager.clear();

        // when
        // 주문: 도서 [100, 200], 카테고리 [10, 20]
        List<Long> resultIds = couponPolicyRepository.findApplicablePolicyIds(
                List.of(100L, 200L),
                List.of(10L, 20L)
        );

        // then
        assertThat(resultIds)
                .hasSize(3) // bookPolicy, categoryPolicy, allPolicy
                .contains(
                        bookPolicy.getCouponPolicyId(),
                        categoryPolicy.getCouponPolicyId(),
                        allPolicy.getCouponPolicyId()
                )
                .doesNotContain(otherBookPolicy.getCouponPolicyId());
    }

    @Test
    @DisplayName("적용 가능한 정책이 없는 경우 빈 리스트 반환")
    void findApplicablePolicyIds_EmptyTest() {
        // given
        CouponPolicy policy = savePolicy("Specific Book", CouponPolicyType.BOOK, CouponPolicyDiscountType.FIXED,
                CouponPolicyStatus.ACTIVE);
        addTargetBook(policy, 999L); // 999번 책만 가능

        entityManager.flush();
        entityManager.clear();

        // when
        // 주문: 도서 100번
        List<Long> resultIds = couponPolicyRepository.findApplicablePolicyIds(
                List.of(100L),
                Collections.emptyList()
        );

        // then
        assertThat(resultIds).isEmpty();
    }
}