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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
class MemberCouponRepositoryTest {

    @Autowired
    private MemberCouponRepository memberCouponRepository;
    @Autowired
    private TestEntityManager entityManager;

    // --- Helper Methods (Builder 사용) ---

    private CouponPolicy createPolicy(String name) {
        CouponPolicy p = CouponPolicy.builder()
                .couponPolicyName(name)
                .couponPolicyType(CouponPolicyType.WELCOME)
                .couponPolicyDiscountType(CouponPolicyDiscountType.FIXED)
                .couponDiscountValue(1000)
                .couponPolicyStatus(CouponPolicyStatus.ACTIVE)
                .minPrice(0)
                .build();
        return entityManager.persist(p);
    }

    private Coupon createCoupon(CouponPolicy policy) {
        Coupon c = Coupon.builder()
                .couponPolicy(policy)
                .couponRemainingQuantity(100) // 필드명: couponRemainingQuantity
                .build();
        return entityManager.persist(c);
    }

    private MemberCoupon createMemberCoupon(Long userId, Coupon coupon, MemberCouponStatus status, String orderNumber) {
        // 기본적으로 유효기간이 30일 남은 쿠폰 생성
        MemberCoupon.MemberCouponBuilder builder = MemberCoupon.builder()
                .userId(userId)
                .coupon(coupon)
                .memberCouponIssuedDate(LocalDateTime.now()) // 필드명: memberCouponIssuedDate
                .memberCouponEndDate(LocalDateTime.now().plusDays(30)) // 필드명: memberCouponEndDate
                .memberCouponStatus(MemberCouponStatus.NOT_USED); // 필드명: memberCouponStatus

        MemberCoupon mc = builder.build();

        // 상태 변경 (비즈니스 메서드 사용)
        if (status == MemberCouponStatus.USED) {
            mc.use(orderNumber);
        } else if (status == MemberCouponStatus.EXPIRED) {
            // 만료 처리 (메서드가 없으면 Reflection 사용)
            ReflectionTestUtils.setField(mc, "memberCouponStatus", MemberCouponStatus.EXPIRED);
        }

        return entityManager.persist(mc);
    }

    // --- Tests ---

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

        createMemberCoupon(userId, c1, MemberCouponStatus.NOT_USED, null);
        createMemberCoupon(userId, c2, MemberCouponStatus.USED, "100L"); // 사용된 쿠폰
        createMemberCoupon(2L, c3, MemberCouponStatus.NOT_USED, null); // 다른 유저

        entityManager.flush();
        entityManager.clear();

        // when
        Page<MemberCoupon> result = memberCouponRepository.findCouponsWithPolicy(
                userId, MemberCouponStatus.NOT_USED, PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCoupon().getCouponPolicy().getCouponPolicyName()).isEqualTo(
                "Policy A");
    }

    @Test
    @DisplayName("내 쿠폰 조회 - 전체 조회")
    void findCouponsWithPolicy_AllTest() {
        // given
        Long userId = 1L;
        CouponPolicy p1 = createPolicy("Policy A");
        CouponPolicy p2 = createPolicy("Policy B");
        Coupon c1 = createCoupon(p1);
        Coupon c2 = createCoupon(p2);

        createMemberCoupon(userId, c1, MemberCouponStatus.NOT_USED, null);
        createMemberCoupon(userId, c2, MemberCouponStatus.USED, "100L");

        entityManager.flush();
        entityManager.clear();

        // when
        Page<MemberCoupon> result = memberCouponRepository.findCouponsWithPolicy(
                userId, null, PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("쿠폰 중복 발급 여부 확인")
    void existsByUserIdAndCoupon_CouponId_Test() {
        // given
        Long userId = 1L;
        CouponPolicy p = createPolicy("P");
        Coupon c = createCoupon(p);

        createMemberCoupon(userId, c, MemberCouponStatus.NOT_USED, null);

        // when
        boolean exists = memberCouponRepository.existsByUserIdAndCoupon_CouponId(userId, c.getCouponId());
        boolean notExists = memberCouponRepository.existsByUserIdAndCoupon_CouponId(userId, 999L);

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("주문 ID로 사용된 쿠폰 찾기")
    void findByorderNumber_Test() {
        // given
        Long userId = 1L;
        String orderNumber = "12345L";
        CouponPolicy p = createPolicy("Policy");
        Coupon c = createCoupon(p);

        createMemberCoupon(userId, c, MemberCouponStatus.USED, orderNumber);

        entityManager.flush();
        entityManager.clear();

        // when
        Optional<MemberCoupon> result = memberCouponRepository.findByOrderNumber(orderNumber);
        Optional<MemberCoupon> notFound = memberCouponRepository.findByOrderNumber("99999L");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getMemberCouponId()).isNotNull();
        assertThat(result.get().getCoupon().getCouponPolicy().getCouponPolicyName()).isEqualTo("Policy");

        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("정책 ID 리스트로 사용 가능한 내 쿠폰 조회 (Reverse Lookup 2단계)")
    void findUsableCouponsByPolicyIds_Test() {
        // given
        Long userId = 1L;
        LocalDateTime now = LocalDateTime.now();

        CouponPolicy p1 = createPolicy("Policy Valid");   // 유효
        CouponPolicy p2 = createPolicy("Policy Used");    // 사용됨
        CouponPolicy p3 = createPolicy("Policy Other");   // 대상 아님
        CouponPolicy p4 = createPolicy("Policy Expired"); // 만료됨

        Coupon c1 = createCoupon(p1);
        Coupon c2 = createCoupon(p2);
        Coupon c3 = createCoupon(p3);
        Coupon c4 = createCoupon(p4);

        MemberCoupon validMc = createMemberCoupon(userId, c1, MemberCouponStatus.NOT_USED, null);

        createMemberCoupon(userId, c2, MemberCouponStatus.USED, "100L");

        createMemberCoupon(userId, c3, MemberCouponStatus.NOT_USED, null);

        createMemberCoupon(2L, c1, MemberCouponStatus.NOT_USED,
                null); // 주의: c1(p1)은 이미 User1이 가졌으므로 1:N 허용 확인 필요. MemberCoupon 테이블은 (UserId, CouponId)가 유니크하므로 User2는 c1 발급 가능.

        // [제외] Policy Expired, 만료됨 (강제로 날짜를 과거로 설정)
        MemberCoupon expiredMc = MemberCoupon.builder()
                .userId(userId)
                .coupon(c4)
                .memberCouponIssuedDate(now.minusDays(30))
                .memberCouponEndDate(now.minusDays(1)) // 어제 날짜로 만료
                .memberCouponStatus(MemberCouponStatus.NOT_USED)
                .build();
        entityManager.persist(expiredMc);

        entityManager.flush();
        entityManager.clear();

        // when
        List<Long> targetPolicyIds = List.of(
                p1.getCouponPolicyId(),
                p2.getCouponPolicyId(),
                p4.getCouponPolicyId()
        );

        List<MemberCoupon> result = memberCouponRepository.findUsableCouponsByPolicyIds(
                userId,
                targetPolicyIds,
                now
        );

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMemberCouponId()).isEqualTo(validMc.getMemberCouponId());
        assertThat(result.get(0).getCoupon().getCouponPolicy().getCouponPolicyName()).isEqualTo("Policy Valid");
    }

    @Test
    @DisplayName("쿠폰 조회 시 정책 및 타겟 정보까지 함께 로딩 (Fetch Join)")
    void findByIdWithTargets_Test() {
        // given
        Long userId = 1L;

        // 1. 정책 생성
        CouponPolicy policy = createPolicy("Target Policy");

        // 2. 타겟 도서 추가 (CouponPolicyTargetBook 엔티티 필요)
        // [주의] 테스트용으로 직접 insert 하거나, 연관관계 메서드 사용해야 함
        // 여기서는 개념적으로 EntityManager를 사용해 직접 넣는다고 가정
        com.example.book2onandoncouponservice.entity.CouponPolicyTargetBook targetBook =
                com.example.book2onandoncouponservice.entity.CouponPolicyTargetBook.builder()
                        .couponPolicy(policy)
                        .bookId(1001L)
                        .build();
        entityManager.persist(targetBook);

        // 3. 타겟 카테고리 추가
        com.example.book2onandoncouponservice.entity.CouponPolicyTargetCategory targetCategory =
                com.example.book2onandoncouponservice.entity.CouponPolicyTargetCategory.builder()
                        .couponPolicy(policy)
                        .categoryId(50L)
                        .build();
        entityManager.persist(targetCategory);

        Coupon coupon = createCoupon(policy);
        MemberCoupon memberCoupon = createMemberCoupon(userId, coupon, MemberCouponStatus.NOT_USED, null);

        entityManager.flush();
        entityManager.clear(); // 1차 캐시 비움 (실제 쿼리 발생 유도)

        // when
        Optional<MemberCoupon> result = memberCouponRepository.findByIdWithTargets(memberCoupon.getMemberCouponId());

        // then
        assertThat(result).isPresent();
        MemberCoupon mc = result.get();

        // 1. 기본 정보 확인
        assertThat(mc.getMemberCouponId()).isEqualTo(memberCoupon.getMemberCouponId());

        // 2. 정책 정보 Lazy Loading 없이 접근 가능한지 확인 (Fetch Join 검증)
        CouponPolicy loadedPolicy = mc.getCoupon().getCouponPolicy();
        assertThat(loadedPolicy.getCouponPolicyName()).isEqualTo("Target Policy");

        // 3. 타겟 도서 목록 확인
        assertThat(loadedPolicy.getCouponPolicyTargetBooks()).isNotEmpty();
        assertThat(loadedPolicy.getCouponPolicyTargetBooks().get(0).getBookId()).isEqualTo(1001L);

        // 4. 타겟 카테고리 목록 확인
        assertThat(loadedPolicy.getCouponPolicyTargetCategories()).isNotEmpty();
        assertThat(loadedPolicy.getCouponPolicyTargetCategories().get(0).getCategoryId()).isEqualTo(50L);
    }
}