package com.example.book2onandoncouponservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.book2onandoncouponservice.dto.request.OrderCouponCheckRequestDto;
import com.example.book2onandoncouponservice.dto.response.CouponTargetResponseDto;
import com.example.book2onandoncouponservice.dto.response.MemberCouponResponseDto;
import com.example.book2onandoncouponservice.entity.Coupon;
import com.example.book2onandoncouponservice.entity.CouponPolicy;
import com.example.book2onandoncouponservice.entity.CouponPolicyDiscountType;
import com.example.book2onandoncouponservice.entity.CouponPolicyTargetBook;
import com.example.book2onandoncouponservice.entity.CouponPolicyTargetCategory;
import com.example.book2onandoncouponservice.entity.MemberCoupon;
import com.example.book2onandoncouponservice.entity.MemberCouponStatus;
import com.example.book2onandoncouponservice.exception.CouponErrorCode;
import com.example.book2onandoncouponservice.exception.CouponIssueException;
import com.example.book2onandoncouponservice.exception.CouponNotFoundException;
import com.example.book2onandoncouponservice.repository.CouponPolicyRepository;
import com.example.book2onandoncouponservice.repository.MemberCouponRepository;
import com.example.book2onandoncouponservice.service.impl.MemberCouponServiceImpl;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MemberCouponServiceTest {

    @InjectMocks
    private MemberCouponServiceImpl memberCouponService;

    @Mock
    private MemberCouponRepository memberCouponRepository;

    @Mock
    private CouponPolicyRepository couponPolicyRepository;

    // --- Helper: DTO 변환을 위한 Mock 객체 생성 ---
    private MemberCoupon createStubbedMemberCoupon(Long id, Long userId) {
        MemberCoupon mc = mock(MemberCoupon.class);
        Coupon coupon = mock(Coupon.class);
        CouponPolicy policy = mock(CouponPolicy.class);

        // DTO 매핑 시 호출되는 메서드들 Stubbing (lenient 사용)
        lenient().when(mc.getCoupon()).thenReturn(coupon);
        lenient().when(coupon.getCouponPolicy()).thenReturn(policy);

        lenient().when(mc.getMemberCouponId()).thenReturn(id);
        lenient().when(mc.getUserId()).thenReturn(userId);
        lenient().when(mc.getMemberCouponStatus()).thenReturn(MemberCouponStatus.NOT_USED);

        lenient().when(policy.getCouponPolicyName()).thenReturn("Test Coupon");
        lenient().when(policy.getCouponPolicyDiscountType()).thenReturn(CouponPolicyDiscountType.FIXED);
        lenient().when(policy.getCouponDiscountValue()).thenReturn(1000);
        lenient().when(policy.getMinPrice()).thenReturn(10000);

        return mc;
    }

    // ==========================================
    // 1. getMyCoupon (내 쿠폰 조회)
    // ==========================================

    @Test
    @DisplayName("내 쿠폰 목록 조회 성공 - 상태 필터링 (USED)")
    void getMyCoupon_WithStatus() {
        // given
        Long userId = 1L;
        String status = "USED";
        Pageable pageable = PageRequest.of(0, 10);

        MemberCoupon mc = createStubbedMemberCoupon(1L, userId);

        given(memberCouponRepository.findCouponsWithPolicy(eq(userId), eq(MemberCouponStatus.USED), any()))
                .willReturn(new PageImpl<>(List.of(mc)));

        // when
        Page<MemberCouponResponseDto> result = memberCouponService.getMyCoupon(userId, pageable, status);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCouponName()).isEqualTo("Test Coupon");
    }

    @Test
    @DisplayName("내 쿠폰 목록 조회 성공 - 전체 조회 (Status Null)")
    void getMyCoupon_All() {
        // given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        given(memberCouponRepository.findCouponsWithPolicy(eq(userId), eq(null), any()))
                .willReturn(new PageImpl<>(List.of()));

        // when
        memberCouponService.getMyCoupon(userId, pageable, null);

        // then
        verify(memberCouponRepository).findCouponsWithPolicy(eq(userId), eq(null), any());
    }

    // ==========================================
    // 2. useMemberCoupon (쿠폰 사용 - orderNumber 추가됨)
    // ==========================================

    @Test
    @DisplayName("쿠폰 사용 성공 - orderNumber 전달 확인")
    void useMemberCoupon_Success() {
        // given
        Long mcId = 1L;
        Long userId = 100L;
        String orderNumber = "12345L"; // 주문 번호

        MemberCoupon memberCoupon = mock(MemberCoupon.class);
        given(memberCouponRepository.findById(mcId)).willReturn(Optional.of(memberCoupon));
        given(memberCoupon.getUserId()).willReturn(userId); // 소유자 일치

        // when
        memberCouponService.useMemberCoupon(mcId, userId, orderNumber);

        // then
        verify(memberCoupon).use(orderNumber); // [핵심] orderNumber가 전달되었는지 확인
    }

    @Test
    @DisplayName("쿠폰 사용 실패 - 쿠폰 없음 (404)")
    void useMemberCoupon_Fail_NotFound() {
        String orderNumber = "12345L";
        given(memberCouponRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberCouponService.useMemberCoupon(1L, 100L, orderNumber))
                .isInstanceOf(CouponNotFoundException.class);
    }

    @Test
    @DisplayName("쿠폰 사용 실패 - 본인 쿠폰 아님 (403)")
    void useMemberCoupon_Fail_NotOwner() {
        Long mcId = 1L;
        Long userId = 100L;
        Long otherUser = 999L;
        String orderNumber = "12345L";

        MemberCoupon memberCoupon = mock(MemberCoupon.class);
        given(memberCouponRepository.findById(mcId)).willReturn(Optional.of(memberCoupon));
        given(memberCoupon.getUserId()).willReturn(otherUser); // 다른 사람

        assertThatThrownBy(() -> memberCouponService.useMemberCoupon(mcId, userId, orderNumber))
                .isInstanceOf(CouponIssueException.class)
                .hasMessage(CouponErrorCode.NOT_COUPON_OWNER.getMessage());
    }

    // ==========================================
    // 3. cancelCouponByOrder (주문 번호로 취소 - 새로 추가된 메서드)
    // ==========================================

    @Test
    @DisplayName("주문 취소로 인한 쿠폰 복구 성공")
    void cancelCouponByOrder_Success() {
        // given
        String orderNumber = "12345L";
        MemberCoupon memberCoupon = mock(MemberCoupon.class);

        // 주문 번호로 조회 성공 가정
        given(memberCouponRepository.findByOrderNumber(orderNumber)).willReturn(Optional.of(memberCoupon));

        given(memberCoupon.getOrderNumber()).willReturn(orderNumber);
        // when
        memberCouponService.cancelMemberCoupon(orderNumber);

        // then
        verify(memberCoupon).cancelUsage(); // 취소 메서드 호출 확인
    }

    @Test
    @DisplayName("주문 취소 실패 - 해당 주문에 사용된 쿠폰 없음")
    void cancelCouponByOrder_Fail_NotFound() {
        String orderNumber = "12345L";
        given(memberCouponRepository.findByOrderNumber(orderNumber)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberCouponService.cancelMemberCoupon(orderNumber))
                .isInstanceOf(CouponNotFoundException.class);
    }

    @Test
    @DisplayName("주문 적용 가능 쿠폰 조회 성공")
    void getUsableCoupons_Success() {
        // given
        Long userId = 1L;
        List<Long> bookIds = List.of(100L, 101L);
        List<Long> categoryIds = List.of(10L);
        OrderCouponCheckRequestDto requestDto = new OrderCouponCheckRequestDto(bookIds, categoryIds);

        // 1. 정책 ID 조회 Mocking
        List<Long> policyIds = List.of(1L, 2L);
        given(couponPolicyRepository.findApplicablePolicyIds(bookIds, categoryIds))
                .willReturn(policyIds);

        // 2. 쿠폰 조회 Mocking (DTO 변환을 위해 Stubbed 객체 사용)
        MemberCoupon mc = createStubbedMemberCoupon(10L, userId);
        given(memberCouponRepository.findUsableCouponsByPolicyIds(eq(userId), eq(policyIds), any()))
                .willReturn(List.of(mc));

        // when
        List<MemberCouponResponseDto> result = memberCouponService.getUsableCoupons(userId, requestDto);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMemberCouponId()).isEqualTo(10L);
        assertThat(result.get(0).getCouponName()).isEqualTo("Test Coupon");

        // Verify
        verify(couponPolicyRepository).findApplicablePolicyIds(bookIds, categoryIds);
        verify(memberCouponRepository).findUsableCouponsByPolicyIds(eq(userId), eq(policyIds), any());
    }

    @Test
    @DisplayName("주문 적용 가능 쿠폰 조회 - 정책 없음 (Early Return)")
    void getUsableCoupons_NoPolicy() {
        // given
        Long userId = 1L;
        OrderCouponCheckRequestDto requestDto = new OrderCouponCheckRequestDto(List.of(999L), List.of(99L));

        // 정책이 없다고 가정
        given(couponPolicyRepository.findApplicablePolicyIds(any(), any()))
                .willReturn(List.of());

        // when
        List<MemberCouponResponseDto> result = memberCouponService.getUsableCoupons(userId, requestDto);

        // then
        assertThat(result).isEmpty(); // 빈 리스트 반환

        // Verify: 정책 조회만 호출되고, 쿠폰 조회는 호출되지 않아야 함 (Early Return 확인)
        verify(couponPolicyRepository).findApplicablePolicyIds(any(), any());
        verify(memberCouponRepository, org.mockito.Mockito.never()).findUsableCouponsByPolicyIds(any(), any(), any());
    }

    @Test
    @DisplayName("쿠폰 적용 대상 조회 성공 - 타겟 도서와 카테고리가 존재하는 경우")
    void getCouponTargets_Success() {
        // given
        Long memberCouponId = 1L;

        CouponPolicy policy = CouponPolicy.builder()
                .couponPolicyDiscountType(CouponPolicyDiscountType.PERCENT)
                .couponDiscountValue(10)
                .minPrice(10000)
                .maxPrice(5000)
                .build();

        CouponPolicyTargetBook targetBook1 = CouponPolicyTargetBook.builder().bookId(101L).couponPolicy(policy).build();
        CouponPolicyTargetBook targetBook2 = CouponPolicyTargetBook.builder().bookId(102L).couponPolicy(policy).build();
        CouponPolicyTargetCategory targetCategory = CouponPolicyTargetCategory.builder().categoryId(55L)
                .couponPolicy(policy).build();

        // Policy 내부에 리스트 주입 (Entity에 Setter가 없다면 ReflectionTestUtils 사용)
        ReflectionTestUtils.setField(policy, "couponPolicyTargetBooks", List.of(targetBook1, targetBook2));
        ReflectionTestUtils.setField(policy, "couponPolicyTargetCategories", List.of(targetCategory));

        // 3. Mock MemberCoupon 연결
        Coupon coupon = Coupon.builder().couponPolicy(policy).build();
        MemberCoupon memberCoupon = MemberCoupon.builder()
                .memberCouponId(memberCouponId) // 빌더에 ID 필드가 있다면 사용
                .coupon(coupon)
                .build();
        // ID 필드가 빌더에 없다면 Reflection으로 주입
        ReflectionTestUtils.setField(memberCoupon, "memberCouponId", memberCouponId);

        // 4. Repository 동작 정의
        given(memberCouponRepository.findByIdWithTargets(memberCouponId))
                .willReturn(Optional.of(memberCoupon));

        // when
        CouponTargetResponseDto result = memberCouponService.getCouponTargets(memberCouponId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getMemberCouponId()).isEqualTo(memberCouponId);

        // 타겟 ID 리스트 검증
        assertThat(result.getTargetBookIds()).hasSize(2).containsExactlyInAnyOrder(101L, 102L);
        assertThat(result.getTargetCategoryIds()).hasSize(1).contains(55L);

        // 할인 정보 검증
        assertThat(result.getDiscountType()).isEqualTo(CouponPolicyDiscountType.PERCENT);
        assertThat(result.getDiscountValue()).isEqualTo(10);
        assertThat(result.getMinPrice()).isEqualTo(10000);

        // 로그 확인용: Repository가 호출되었는지 검증
        verify(memberCouponRepository).findByIdWithTargets(memberCouponId);
    }

    @Test
    @DisplayName("쿠폰 적용 대상 조회 성공 - 타겟이 없는 전체 적용 쿠폰")
    void getCouponTargets_Success_NoTargets() {
        // given
        Long memberCouponId = 2L;

        CouponPolicy policy = CouponPolicy.builder()
                .couponPolicyDiscountType(CouponPolicyDiscountType.FIXED)
                .couponDiscountValue(2000)
                .build();
        // 타겟 리스트는 null 또는 빈 리스트 상태

        Coupon coupon = Coupon.builder().couponPolicy(policy).build();
        MemberCoupon memberCoupon = MemberCoupon.builder().coupon(coupon).build();
        ReflectionTestUtils.setField(memberCoupon, "memberCouponId", memberCouponId);

        given(memberCouponRepository.findByIdWithTargets(memberCouponId))
                .willReturn(Optional.of(memberCoupon));

        // when
        CouponTargetResponseDto result = memberCouponService.getCouponTargets(memberCouponId);

        // then
        assertThat(result.getTargetBookIds()).isEmpty();
        assertThat(result.getTargetCategoryIds()).isEmpty();
        assertThat(result.getDiscountValue()).isEqualTo(2000);
    }

    @Test
    @DisplayName("쿠폰 적용 대상 조회 실패 - 쿠폰 ID가 존재하지 않음")
    void getCouponTargets_Fail_NotFound() {
        // given
        Long invalidId = 999L;
        given(memberCouponRepository.findByIdWithTargets(invalidId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberCouponService.getCouponTargets(invalidId))
                .isInstanceOf(CouponNotFoundException.class);
    }
}