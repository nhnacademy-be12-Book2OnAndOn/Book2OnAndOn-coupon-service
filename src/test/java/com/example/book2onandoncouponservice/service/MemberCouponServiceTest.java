package com.example.book2onandoncouponservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.book2onandoncouponservice.dto.response.MemberCouponResponseDto;
import com.example.book2onandoncouponservice.entity.Coupon;
import com.example.book2onandoncouponservice.entity.CouponPolicy;
import com.example.book2onandoncouponservice.entity.CouponPolicyDiscountType;
import com.example.book2onandoncouponservice.entity.MemberCoupon;
import com.example.book2onandoncouponservice.entity.MemberCouponStatus;
import com.example.book2onandoncouponservice.exception.CouponErrorCode;
import com.example.book2onandoncouponservice.exception.CouponIssueException;
import com.example.book2onandoncouponservice.exception.CouponNotFoundException;
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

@ExtendWith(MockitoExtension.class)
class MemberCouponServiceTest {

    @InjectMocks
    private MemberCouponServiceImpl memberCouponService;

    @Mock
    private MemberCouponRepository memberCouponRepository;

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
    // 2. useMemberCoupon (쿠폰 사용 - orderId 추가됨)
    // ==========================================

    @Test
    @DisplayName("쿠폰 사용 성공 - orderId 전달 확인")
    void useMemberCoupon_Success() {
        // given
        Long mcId = 1L;
        Long userId = 100L;
        Long orderId = 12345L; // 주문 번호

        MemberCoupon memberCoupon = mock(MemberCoupon.class);
        given(memberCouponRepository.findById(mcId)).willReturn(Optional.of(memberCoupon));
        given(memberCoupon.getUserId()).willReturn(userId); // 소유자 일치

        // when
        memberCouponService.useMemberCoupon(mcId, userId, orderId);

        // then
        verify(memberCoupon).use(orderId); // [핵심] orderId가 전달되었는지 확인
    }

    @Test
    @DisplayName("쿠폰 사용 실패 - 쿠폰 없음 (404)")
    void useMemberCoupon_Fail_NotFound() {
        Long orderId = 12345L;
        given(memberCouponRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberCouponService.useMemberCoupon(1L, 100L, orderId))
                .isInstanceOf(CouponNotFoundException.class);
    }

    @Test
    @DisplayName("쿠폰 사용 실패 - 본인 쿠폰 아님 (403)")
    void useMemberCoupon_Fail_NotOwner() {
        Long mcId = 1L;
        Long userId = 100L;
        Long otherUser = 999L;
        Long orderId = 12345L;

        MemberCoupon memberCoupon = mock(MemberCoupon.class);
        given(memberCouponRepository.findById(mcId)).willReturn(Optional.of(memberCoupon));
        given(memberCoupon.getUserId()).willReturn(otherUser); // 다른 사람

        assertThatThrownBy(() -> memberCouponService.useMemberCoupon(mcId, userId, orderId))
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
        Long orderId = 12345L;
        MemberCoupon memberCoupon = mock(MemberCoupon.class);

        // 주문 번호로 조회 성공 가정
        given(memberCouponRepository.findByOrderId(orderId)).willReturn(Optional.of(memberCoupon));

        given(memberCoupon.getOrderId()).willReturn(orderId);
        // when
        memberCouponService.cancelMemberCoupon(orderId);

        // then
        verify(memberCoupon).cancelUsage(); // 취소 메서드 호출 확인
    }

    @Test
    @DisplayName("주문 취소 실패 - 해당 주문에 사용된 쿠폰 없음")
    void cancelCouponByOrder_Fail_NotFound() {
        Long orderId = 12345L;
        given(memberCouponRepository.findByOrderId(orderId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberCouponService.cancelMemberCoupon(orderId))
                .isInstanceOf(CouponNotFoundException.class);
    }
}