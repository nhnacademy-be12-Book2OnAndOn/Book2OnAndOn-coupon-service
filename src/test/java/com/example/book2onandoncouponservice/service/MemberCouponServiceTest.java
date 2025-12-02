package com.example.book2onandoncouponservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
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
import com.example.book2onandoncouponservice.exception.CouponUseException;
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

    // --- Helper Method (DTO 변환을 위한 Mock 객체 생성) ---
    private MemberCoupon createStubbedMemberCoupon(Long id, Long userId) {
        // 1. 껍데기 Mock 생성
        MemberCoupon mc = mock(MemberCoupon.class);
        Coupon coupon = mock(Coupon.class);
        CouponPolicy policy = mock(CouponPolicy.class);

        // 2. 연관 관계 Stubbing (getCoupon().getPolicy()... 체이닝 방지)
        lenient().when(mc.getCoupon()).thenReturn(coupon);
        lenient().when(coupon.getCouponPolicy()).thenReturn(policy);

        // 3. DTO 생성에 필요한 값 Stubbing
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
        Long userId = 1L;
        String status = "USED";
        Pageable pageable = PageRequest.of(0, 10);

        MemberCoupon mc = createStubbedMemberCoupon(1L, userId);

        // Repository가 USED 상태로 조회하도록 호출되는지 검증
        given(memberCouponRepository.findCouponsWithPolicy(eq(userId), eq(MemberCouponStatus.USED), any()))
                .willReturn(new PageImpl<>(List.of(mc)));

        Page<MemberCouponResponseDto> result = memberCouponService.getMyCoupon(userId, pageable, status);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCouponName()).isEqualTo("Test Coupon");
    }

    @Test
    @DisplayName("내 쿠폰 목록 조회 성공 - 전체 조회 (Status Null)")
    void getMyCoupon_All() {
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        given(memberCouponRepository.findCouponsWithPolicy(eq(userId), eq(null), any()))
                .willReturn(new PageImpl<>(List.of()));

        memberCouponService.getMyCoupon(userId, pageable, null);

        verify(memberCouponRepository).findCouponsWithPolicy(eq(userId), eq(null), any());
    }

    @Test
    @DisplayName("내 쿠폰 목록 조회 성공 - 잘못된 상태값은 전체 조회로 처리")
    void getMyCoupon_InvalidStatus() {
        Long userId = 1L;
        String status = "INVALID_STATUS";
        Pageable pageable = PageRequest.of(0, 10);

        given(memberCouponRepository.findCouponsWithPolicy(eq(userId), eq(null), any()))
                .willReturn(new PageImpl<>(List.of()));

        memberCouponService.getMyCoupon(userId, pageable, status);

        verify(memberCouponRepository).findCouponsWithPolicy(eq(userId), eq(null), any());
    }

    // ==========================================
    // 2. useMemberCoupon (쿠폰 사용)
    // ==========================================

    @Test
    @DisplayName("쿠폰 사용 성공")
    void useMemberCoupon_Success() {
        Long mcId = 1L;
        Long userId = 100L;

        // Mock 객체 생성
        MemberCoupon memberCoupon = mock(MemberCoupon.class);
        given(memberCouponRepository.findById(mcId)).willReturn(Optional.of(memberCoupon));
        given(memberCoupon.getUserId()).willReturn(userId); // 소유자 일치

        // when
        memberCouponService.useMemberCoupon(mcId, userId);

        // then
        verify(memberCoupon).use(); // 엔티티의 use() 메서드가 호출되었는지 확인
    }

    @Test
    @DisplayName("쿠폰 사용 실패 - 쿠폰 없음 (404)")
    void useMemberCoupon_Fail_NotFound() {
        given(memberCouponRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberCouponService.useMemberCoupon(1L, 100L))
                .isInstanceOf(CouponNotFoundException.class);
    }

    @Test
    @DisplayName("쿠폰 사용 실패 - 본인 쿠폰 아님 (403)")
    void useMemberCoupon_Fail_NotOwner() {
        Long mcId = 1L;
        Long userId = 100L;
        Long otherUserId = 999L;

        MemberCoupon memberCoupon = mock(MemberCoupon.class);
        given(memberCouponRepository.findById(mcId)).willReturn(Optional.of(memberCoupon));
        given(memberCoupon.getUserId()).willReturn(otherUserId); // 소유자 불일치

        assertThatThrownBy(() -> memberCouponService.useMemberCoupon(mcId, userId))
                .isInstanceOf(CouponIssueException.class)
                .hasMessage(CouponErrorCode.NOT_COUPON_OWNER.getMessage());
    }

    @Test
    @DisplayName("쿠폰 사용 실패 - 이미 사용됨/만료됨 (Entity 예외 전파 확인)")
    void useMemberCoupon_Fail_AlreadyUsed() {
        Long mcId = 1L;
        Long userId = 100L;
        MemberCoupon memberCoupon = mock(MemberCoupon.class);

        given(memberCouponRepository.findById(mcId)).willReturn(Optional.of(memberCoupon));
        given(memberCoupon.getUserId()).willReturn(userId);

        // Entity.use()에서 예외가 터지는 상황 시뮬레이션
        doThrow(new CouponUseException(CouponErrorCode.COUPON_ALREADY_USED))
                .when(memberCoupon).use();

        assertThatThrownBy(() -> memberCouponService.useMemberCoupon(mcId, userId))
                .isInstanceOf(CouponUseException.class)
                .hasMessage(CouponErrorCode.COUPON_ALREADY_USED.getMessage());
    }

    // ==========================================
    // 3. cancelMemberCoupon (사용 취소)
    // ==========================================

    @Test
    @DisplayName("쿠폰 사용 취소 성공")
    void cancelMemberCoupon_Success() {
        Long mcId = 1L;
        MemberCoupon memberCoupon = mock(MemberCoupon.class);
        given(memberCouponRepository.findById(mcId)).willReturn(Optional.of(memberCoupon));

        // when
        memberCouponService.cancelMemberCoupon(mcId, 1L);

        // then
        verify(memberCoupon).cancelUsage(); // cancelUsage() 호출 확인
    }

    @Test
    @DisplayName("쿠폰 사용 취소 실패 - 존재하지 않는 쿠폰")
    void cancelMemberCoupon_Fail_NotFound() {
        given(memberCouponRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberCouponService.cancelMemberCoupon(1L, 1L))
                .isInstanceOf(CouponNotFoundException.class);
    }
}