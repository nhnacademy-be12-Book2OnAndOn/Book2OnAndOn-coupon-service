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

    private MemberCoupon createMemberCoupon(Long userId, Coupon coupon, MemberCouponStatus status) {
        MemberCoupon mc = MemberCoupon.builder()
                .userId(userId)
                .coupon(coupon)
                .issuedDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(30))
                .build();

        if (status == MemberCouponStatus.USED) {
            mc.use();
        } else if (status == MemberCouponStatus.EXPIRED) {
            mc.expired();
        }

        return entityManager.persist(mc);
    }

    @Test
    @DisplayName("내 쿠폰 조회 - 상태 필터링 및 Fetch Join 확인")
    void findCouponsWithPolicy_StatusTest() {
        // given
        Long userId = 1L;

        CouponPolicy p1 = createPolicy("Policy A");
        CouponPolicy p2 = createPolicy("Policy B");
        CouponPolicy p3 = createPolicy("Policy C");

        Coupon c1 = createCoupon(p1);
        Coupon c2 = createCoupon(p2);
        Coupon c3 = createCoupon(p3);

        createMemberCoupon(userId, c1, MemberCouponStatus.NOT_USED); // 조회 대상
        createMemberCoupon(userId, c2, MemberCouponStatus.USED);     // 필터링 대상
        createMemberCoupon(2L, c3, MemberCouponStatus.NOT_USED);     // 다른 유저

        entityManager.flush();
        entityManager.clear();

        // when (userId=1, status=NOT_USED)
        Page<MemberCoupon> result = memberCouponRepository.findCouponsWithPolicy(
                userId,
                MemberCouponStatus.NOT_USED,
                PageRequest.of(0, 10)
        );

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCoupon().getCouponPolicy().getCouponPolicyName()).isEqualTo(
                "Policy A");
    }

    @Test
    @DisplayName("내 쿠폰 조회 - 전체 조회 (status = null)")
    void findCouponsWithPolicy_AllTest() {
        // given
        Long userId = 1L;

        CouponPolicy p1 = createPolicy("Policy A");
        CouponPolicy p2 = createPolicy("Policy B");

        // 서로 다른 정책으로 쿠폰 생성
        Coupon c1 = createCoupon(p1);
        Coupon c2 = createCoupon(p2);

        // 사용자에게 쿠폰 지급
        createMemberCoupon(userId, c1, MemberCouponStatus.NOT_USED);
        createMemberCoupon(userId, c2, MemberCouponStatus.USED);

        entityManager.flush();
        entityManager.clear();

        // when
        Page<MemberCoupon> result = memberCouponRepository.findCouponsWithPolicy(
                userId,
                null,
                PageRequest.of(0, 10)
        );

        // then
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("쿠폰 중복 발급 여부 확인")
    void existsByUserIdAndCoupon_CouponId_Test() {
        // given
        Long userId = 1L;
        CouponPolicy p = createPolicy("P");
        Coupon c = createCoupon(p); // ID 자동 생성
        Long couponId = c.getCouponId();

        createMemberCoupon(userId, c, MemberCouponStatus.NOT_USED);

        // when
        boolean exists = memberCouponRepository.existsByUserIdAndCoupon_CouponId(userId, couponId);
        boolean notExists = memberCouponRepository.existsByUserIdAndCoupon_CouponId(userId, 999L);

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }
}