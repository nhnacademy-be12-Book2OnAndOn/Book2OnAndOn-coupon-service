package com.example.book2onandoncouponservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.book2onandoncouponservice.entity.Coupon;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class CouponRepositoryTest {

    @Autowired
    private CouponRepository couponRepository;
    @Autowired
    private TestEntityManager entityManager;

    // --- Helper methods ---
    private CouponPolicy createPolicy(CouponPolicyType type, CouponPolicyStatus status) {
        CouponPolicy p = new CouponPolicy();
        p.setCouponPolicyName("Policy " + type);
        p.setCouponPolicyType(type);
        p.setCouponPolicyStatus(status);
        p.setCouponPolicyDiscountType(CouponPolicyDiscountType.FIXED);
        p.setCouponDiscountValue(1000);
        p.setMinPrice(0);
        return entityManager.persist(p);
    }

    private void createCoupon(CouponPolicy policy) {
        Coupon c = new Coupon(100, policy);
        entityManager.persist(c);
    }

    private void createTargetBook(CouponPolicy policy, Long bookId) {
        CouponPolicyTargetBook target = CouponPolicyTargetBook.builder()
                .couponPolicy(policy)
                .bookId(bookId)
                .build();
        entityManager.persist(target);
    }

    private void createTargetCategory(CouponPolicy policy, Long categoryId) {
        CouponPolicyTargetCategory target = CouponPolicyTargetCategory.builder()
                .couponPolicy(policy)
                .categoryId(categoryId)
                .build();
        entityManager.persist(target);
    }

    // 1. 관리자용 조회 테스트 (상태 필터링)
    @Test
    @DisplayName("관리자 조회 - 상태 필터링 및 Fetch Join 확인")
    void findAllByPolicyStatus_Test() {
        // given
        CouponPolicy p1 = createPolicy(CouponPolicyType.WELCOME, CouponPolicyStatus.ACTIVE);
        createCoupon(p1);

        CouponPolicy p2 = createPolicy(CouponPolicyType.BOOK, CouponPolicyStatus.DEACTIVE);
        createCoupon(p2);

        entityManager.flush();
        entityManager.clear();

        // when (ACTIVE 조회)
        Page<Coupon> result = couponRepository.findAllByPolicyStatus(CouponPolicyStatus.ACTIVE, PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCouponPolicy().getCouponPolicyType()).isEqualTo(
                CouponPolicyType.WELCOME);
    }

    // 2. 적용 가능 쿠폰 조회 테스트 (도서 상세페이지용)
    @Test
    @DisplayName("적용 가능 쿠폰 조회 - BOOK, CATEGORY, CUSTOM은 포함하고 WELCOME, BIRTHDAY는 제외")
    void findAppliableCoupons_ComplexTest() {
        // given
        Long targetBookId = 101L;
        Long otherBookId = 999L;
        Long targetCategoryId = 10L;

        // 1. [매칭 O] 도서 지정 정책 (BOOK, ACTIVE)
        CouponPolicy pBook = createPolicy(CouponPolicyType.BOOK, CouponPolicyStatus.ACTIVE);
        createTargetBook(pBook, targetBookId);
        createCoupon(pBook);

        // 2. [매칭 O] 카테고리 지정 정책 (CATEGORY, ACTIVE)
        CouponPolicy pCat = createPolicy(CouponPolicyType.CATEGORY, CouponPolicyStatus.ACTIVE);
        createTargetCategory(pCat, targetCategoryId);
        createCoupon(pCat);

        // 3. [매칭 O] 전체 대상 이벤트 정책 (CUSTOM, ACTIVE) -> 조회 되어야 함!
        CouponPolicy pCustom = createPolicy(CouponPolicyType.CUSTOM, CouponPolicyStatus.ACTIVE);
        createCoupon(pCustom);

        // 4. [매칭 X] 자동 발급 정책 (WELCOME, ACTIVE) -> 조회 되면 안 됨!
        CouponPolicy pWelcome = createPolicy(CouponPolicyType.WELCOME, CouponPolicyStatus.ACTIVE);
        createCoupon(pWelcome);

        // 5. [매칭 X] 자동 발급 정책 (BIRTHDAY, ACTIVE) -> 조회 되면 안 됨!
        CouponPolicy pBirthday = createPolicy(CouponPolicyType.BIRTHDAY, CouponPolicyStatus.ACTIVE);
        createCoupon(pBirthday);

        // 6. [매칭 X] 다른 도서 지정 (BOOK)
        CouponPolicy pOtherBook = createPolicy(CouponPolicyType.BOOK, CouponPolicyStatus.ACTIVE);
        createTargetBook(pOtherBook, otherBookId);
        createCoupon(pOtherBook);

        // 7. [매칭 X] 조건은 맞으나 비활성 (DEACTIVE)
        CouponPolicy pDeactive = createPolicy(CouponPolicyType.BOOK, CouponPolicyStatus.DEACTIVE);
        createTargetBook(pDeactive, targetBookId);
        createCoupon(pDeactive);

        entityManager.flush();
        entityManager.clear();

        // when
        List<Coupon> results = couponRepository.findAppliableCoupons(targetBookId, List.of(targetCategoryId));

        // then
        // 기대 결과: BOOK, CATEGORY, CUSTOM -> 총 3개
        assertThat(results).hasSize(3);

        List<CouponPolicyType> types = results.stream()
                .map(c -> c.getCouponPolicy().getCouponPolicyType())
                .toList();

        // 포함되어야 할 것들
        assertThat(types).contains(CouponPolicyType.BOOK, CouponPolicyType.CATEGORY, CouponPolicyType.CUSTOM);

        // 제외되어야 할 것들 (자동 발급)
        assertThat(types).doesNotContain(CouponPolicyType.WELCOME, CouponPolicyType.BIRTHDAY);
    }
}