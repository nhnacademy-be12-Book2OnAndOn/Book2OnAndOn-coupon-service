package com.example.book2onandoncouponservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.book2onandoncouponservice.entity.Coupon;
import com.example.book2onandoncouponservice.entity.CouponPolicy;
import com.example.book2onandoncouponservice.entity.CouponPolicyDiscountType;
import com.example.book2onandoncouponservice.entity.CouponPolicyStatus;
import com.example.book2onandoncouponservice.entity.CouponPolicyType;
import com.example.book2onandoncouponservice.entity.MemberCoupon;
import com.example.book2onandoncouponservice.entity.MemberCouponStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class MemberCouponRepositoryTest {

    @Autowired
    private MemberCouponRepository memberCouponRepository;
    @Autowired
    private TestEntityManager entityManager;

    // --- Helper Methods ---
    private CouponPolicy createPolicy(String name) {
        CouponPolicy p = new CouponPolicy();
        p.setCouponPolicyName(name);
        p.setCouponPolicyType(CouponPolicyType.WELCOME);
        p.setCouponPolicyDiscountType(CouponPolicyDiscountType.FIXED);
        p.setCouponDiscountValue(1000);
        p.setCouponPolicyStatus(CouponPolicyStatus.ACTIVE);
        p.setMinPrice(0);
        return entityManager.persist(p);
    }

    private Coupon createCoupon(CouponPolicy policy) {
        Coupon c = new Coupon(100, policy);
        return entityManager.persist(c);
    }

    private MemberCoupon createMemberCoupon(Long userId, Coupon coupon, MemberCouponStatus status, Long orderId) {
        MemberCoupon mc = MemberCoupon.builder()
                .userId(userId)
                .coupon(coupon)
                .issuedDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(30))
                .build();

        if (status == MemberCouponStatus.USED) {
            mc.use(orderId); // 주문 번호와 함께 사용
        } else if (status == MemberCouponStatus.EXPIRED) {
            mc.expired();
        }

        return entityManager.persist(mc);
    }

    // 기존 테스트 1
    @Test
    @DisplayName("내 쿠폰 조회 - 상태 필터링 및 Fetch Join 확인")
    void findCouponsWithPolicy_StatusTest() {
        Long userId = 1L;

        CouponPolicy p1 = createPolicy("Policy A");
        CouponPolicy p2 = createPolicy("Policy B");
        CouponPolicy p3 = createPolicy("Policy C");

        Coupon c1 = createCoupon(p1);
        Coupon c2 = createCoupon(p2);
        Coupon c3 = createCoupon(p3);

        createMemberCoupon(userId, c1, MemberCouponStatus.NOT_USED, null);
        createMemberCoupon(userId, c2, MemberCouponStatus.USED, 100L); // 사용된 쿠폰
        createMemberCoupon(2L, c3, MemberCouponStatus.NOT_USED, null);

        entityManager.flush();
        entityManager.clear();

        Page<MemberCoupon> result = memberCouponRepository.findCouponsWithPolicy(
                userId, MemberCouponStatus.NOT_USED, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCoupon().getCouponPolicy().getCouponPolicyName()).isEqualTo(
                "Policy A");
    }

    // 기존 테스트 2
    @Test
    @DisplayName("내 쿠폰 조회 - 전체 조회")
    void findCouponsWithPolicy_AllTest() {
        Long userId = 1L;
        CouponPolicy p1 = createPolicy("Policy A");
        CouponPolicy p2 = createPolicy("Policy B");
        Coupon c1 = createCoupon(p1);
        Coupon c2 = createCoupon(p2);

        createMemberCoupon(userId, c1, MemberCouponStatus.NOT_USED, null);
        createMemberCoupon(userId, c2, MemberCouponStatus.USED, 100L);

        entityManager.flush();
        entityManager.clear();

        Page<MemberCoupon> result = memberCouponRepository.findCouponsWithPolicy(
                userId, null, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
    }

    // 기존 테스트 3
    @Test
    @DisplayName("쿠폰 중복 발급 여부 확인")
    void existsByUserIdAndCoupon_CouponId_Test() {
        Long userId = 1L;
        CouponPolicy p = createPolicy("P");
        Coupon c = createCoupon(p);

        createMemberCoupon(userId, c, MemberCouponStatus.NOT_USED, null);

        boolean exists = memberCouponRepository.existsByUserIdAndCoupon_CouponId(userId, c.getCouponId());
        boolean notExists = memberCouponRepository.existsByUserIdAndCoupon_CouponId(userId, 999L);

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    // [추가] 주문 ID로 쿠폰 찾기 테스트
    @Test
    @DisplayName("주문 ID로 사용된 쿠폰 찾기")
    void findByOrderId_Test() {
        // given
        Long userId = 1L;
        Long orderId = 12345L;
        CouponPolicy p = createPolicy("Policy");
        Coupon c = createCoupon(p);

        createMemberCoupon(userId, c, MemberCouponStatus.USED, orderId); // orderId=12345로 사용됨

        entityManager.flush();
        entityManager.clear();

        // when
        Optional<MemberCoupon> result = memberCouponRepository.findByOrderId(orderId);
        Optional<MemberCoupon> notFound = memberCouponRepository.findByOrderId(99999L);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getMemberCouponId()).isNotNull();
        assertThat(result.get().getCoupon().getCouponPolicy().getCouponPolicyName()).isEqualTo("Policy");

        assertThat(notFound).isEmpty();
    }
}