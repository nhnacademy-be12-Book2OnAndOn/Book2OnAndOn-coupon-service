package com.example.book2onandoncouponservice.listener;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.book2onandoncouponservice.exception.CouponNotFoundException;
import com.example.book2onandoncouponservice.messaging.consumer.CouponCancelListener;
import com.example.book2onandoncouponservice.service.MemberCouponService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CouponCancelListenerTest {

    @Mock
    private MemberCouponService memberCouponService;

    @InjectMocks
    private CouponCancelListener couponCancelListener;

    @Test
    @DisplayName("쿠폰 취소(롤백) 성공")
    void receive_Success() {
        // given
        Long orderId = 100L;

        // when
        couponCancelListener.receive(orderId);

        // then
        verify(memberCouponService, times(1)).cancelMemberCoupon(orderId);
    }

    @Test
    @DisplayName("비즈니스 예외 발생 시 예외 무시 (CouponNotFoundException)")
    void receive_BusinessException() {
        // given
        Long orderId = 100L;
        doThrow(new CouponNotFoundException())
                .when(memberCouponService).cancelMemberCoupon(orderId);

        // when & then
        assertDoesNotThrow(() -> couponCancelListener.receive(orderId));
    }

    @Test
    @DisplayName("시스템 예외 발생 시 예외 던짐")
    void receive_SystemException() {
        // given
        Long orderId = 100L;
        doThrow(new RuntimeException("시스템 에러"))
                .when(memberCouponService).cancelMemberCoupon(orderId);

        // when & then
        assertThrows(RuntimeException.class, () -> couponCancelListener.receive(orderId));
    }
}