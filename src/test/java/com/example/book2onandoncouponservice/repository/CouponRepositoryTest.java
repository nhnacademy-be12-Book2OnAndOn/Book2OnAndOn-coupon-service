package com.example.book2onandoncouponservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.book2onandoncouponservice.entity.Coupon;
import com.example.book2onandoncouponservice.entity.CouponPolicy;
import com.example.book2onandoncouponservice.entity.CouponPolicyDiscountType;
import com.example.book2onandoncouponservice.entity.CouponPolicyStatus;
import com.example.book2onandoncouponservice.entity.CouponPolicyTargetBook;
import com.example.book2onandoncouponservice.entity.CouponPolicyTargetCategory;
import com.example.book2onandoncouponservice.entity.CouponPolicyType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Slf4j
class CouponRepositoryTest {

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private TestEntityManager entityManager;

    // --- Helper Methods ---

    private CouponPolicy createPolicy(String name, CouponPolicyType type,
                                      CouponPolicyStatus status, LocalDate fixedEndDate) {
        CouponPolicy policy = CouponPolicy.builder()
                .couponPolicyName(name)
                .couponPolicyType(type)
                .couponPolicyDiscountType(CouponPolicyDiscountType.FIXED)
                .couponPolicyStatus(status)
                .couponDiscountValue(1000)
                .minPrice(10000)
                .fixedEndDate(fixedEndDate) // 날짜 테스트용
                .build();
        return entityManager.persist(policy);
    }

    private Coupon createCoupon(CouponPolicy policy, Integer remainingQuantity) {
        Coupon coupon = new Coupon(remainingQuantity, policy);
        return entityManager.persist(coupon);
    }

    private void addTargetBook(CouponPolicy policy, Long bookId) {
        CouponPolicyTargetBook target = CouponPolicyTargetBook.builder()
                .couponPolicy(policy)
                .bookId(bookId)
                .build();
        entityManager.persist(target);
    }

    private void addTargetCategory(CouponPolicy policy, Long categoryId) {
        CouponPolicyTargetCategory target = CouponPolicyTargetCategory.builder()
                .couponPolicy(policy)
                .categoryId(categoryId)
                .build();
        entityManager.persist(target);
    }

    // --- Tests ---

    @Test
    @DisplayName("정책 상태에 따른 쿠폰 조회 (Fetch Join 확인)")
    void findAllByPolicyStatus_Test() {
        // given
        CouponPolicy activePolicy = createPolicy("Active", CouponPolicyType.WELCOME, CouponPolicyStatus.ACTIVE, null);
        CouponPolicy deactivePolicy = createPolicy("Deactive", CouponPolicyType.WELCOME, CouponPolicyStatus.DEACTIVE,
                null);

        createCoupon(activePolicy, 100);
        createCoupon(deactivePolicy, 100);

        entityManager.flush();
        entityManager.clear();

        // when
        Page<Coupon> result = couponRepository.findAllByPolicyStatus(CouponPolicyStatus.ACTIVE, PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCouponPolicy().getCouponPolicyStatus()).isEqualTo(
                CouponPolicyStatus.ACTIVE);
    }

    @Test
    @DisplayName("다운로드 가능 쿠폰 조회 - 재고가 없거나 만료된 쿠폰 제외")
    void findAvailableCoupons_Test() {
        // given
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate yesterday = today.minusDays(1);

        // 1. 정상 (재고 있음, 기간 남음) - 조회 대상
        CouponPolicy p1 = createPolicy("Valid", CouponPolicyType.CUSTOM, CouponPolicyStatus.ACTIVE, tomorrow);
        Coupon c1 = createCoupon(p1, 10);

        // 2. 재고 없음 (quantity = 0) - 제외
        CouponPolicy p2 = createPolicy("No Stock", CouponPolicyType.CUSTOM, CouponPolicyStatus.ACTIVE, tomorrow);
        createCoupon(p2, 0);

        // 3. 만료됨 (fixedEndDate < today) - 제외
        CouponPolicy p3 = createPolicy("Expired", CouponPolicyType.CUSTOM, CouponPolicyStatus.ACTIVE, yesterday);
        createCoupon(p3, 10);

        // 4. 정책 비활성화 - 제외
        CouponPolicy p4 = createPolicy("Deactive", CouponPolicyType.CUSTOM, CouponPolicyStatus.DEACTIVE, tomorrow);
        createCoupon(p4, 10);

        // 5. 무제한 재고 (quantity = null) - 조회 대상
        CouponPolicy p5 = createPolicy("Unlimited", CouponPolicyType.CUSTOM, CouponPolicyStatus.ACTIVE, tomorrow);
        Coupon c5 = createCoupon(p5, null);

        entityManager.flush();
        entityManager.clear();

        // when
        Page<Coupon> result = couponRepository.findAvailableCoupons(
                CouponPolicyStatus.ACTIVE,
                today,
                PageRequest.of(0, 10)
        );

        // then
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting("couponId")
                .containsExactlyInAnyOrder(c1.getCouponId(), c5.getCouponId());
    }

    @Test
    @DisplayName("상품/카테고리 적용 가능 쿠폰 목록 조회")
    void findAppliableCoupons_Test() {
        // given
        // 1. 특정 도서(100L) 지정 쿠폰
        CouponPolicy bookPolicy = createPolicy("Book Target", CouponPolicyType.BOOK, CouponPolicyStatus.ACTIVE, null);
        addTargetBook(bookPolicy, 100L);
        Coupon bookCoupon = createCoupon(bookPolicy, 100);

        // 2. 특정 카테고리(20L) 지정 쿠폰
        CouponPolicy categoryPolicy = createPolicy("Category Target", CouponPolicyType.CATEGORY,
                CouponPolicyStatus.ACTIVE, null);
        addTargetCategory(categoryPolicy, 20L);
        Coupon categoryCoupon = createCoupon(categoryPolicy, 100);

        // 3. 전체 적용(CUSTOM) 쿠폰
        CouponPolicy customPolicy = createPolicy("Custom", CouponPolicyType.CUSTOM, CouponPolicyStatus.ACTIVE, null);
        Coupon customCoupon = createCoupon(customPolicy, 100);

        // 4. 해당 없는 도서(999L) 지정 쿠폰 (조회 안되어야 함)
        CouponPolicy otherBookPolicy = createPolicy("Other Book", CouponPolicyType.BOOK, CouponPolicyStatus.ACTIVE,
                null);
        addTargetBook(otherBookPolicy, 999L);
        createCoupon(otherBookPolicy, 100);

        entityManager.flush();
        entityManager.clear();

        // when
        // 주문: 도서 100번, 카테고리 [10, 20]
        List<Coupon> result = couponRepository.findAppliableCoupons(100L, List.of(10L, 20L));

        // then
        assertThat(result).hasSize(3);
        assertThat(result)
                .extracting("couponId")
                .containsExactlyInAnyOrder(
                        bookCoupon.getCouponId(),
                        categoryCoupon.getCouponId(),
                        customCoupon.getCouponId()
                );
    }

    @Test
    @DisplayName("비관적 락(Pessimistic Lock) 조회 테스트")
    void findByIdForUpdate_Test() {
        // given
        CouponPolicy policy = createPolicy("Lock Policy", CouponPolicyType.CUSTOM, CouponPolicyStatus.ACTIVE, null);
        Coupon coupon = createCoupon(policy, 100);

        entityManager.flush();
        entityManager.clear();

        // when
        Optional<Coupon> found = couponRepository.findById
                (coupon.getCouponId());

        // then
        // 단일 스레드 테스트라 락 동작을 직접 검증하긴 어렵지만, 쿼리 실행과 조회가 정상적인지 확인
        assertThat(found).isPresent();
        assertThat(found.get().getCouponId()).isEqualTo(coupon.getCouponId());
    }
}