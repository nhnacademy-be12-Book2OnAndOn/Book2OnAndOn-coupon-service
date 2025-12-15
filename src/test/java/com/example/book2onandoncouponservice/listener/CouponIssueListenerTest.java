package com.example.book2onandoncouponservice.listener;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.book2onandoncouponservice.exception.CouponErrorCode;
import com.example.book2onandoncouponservice.exception.CouponIssueException;
import com.example.book2onandoncouponservice.messaging.CouponIssueMessage;
import com.example.book2onandoncouponservice.messaging.consumer.CouponIssueListener;
import com.example.book2onandoncouponservice.service.CouponService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CouponIssueListenerTest {

    @Mock
    private CouponService couponService;

    @InjectMocks
    private CouponIssueListener couponIssueListener;

    @Test
    @DisplayName("쿠폰 발급 성공")
    void receive_Success() {
        // given
        CouponIssueMessage message = mock(CouponIssueMessage.class);
        when(message.userId()).thenReturn(1L);
        when(message.couponId()).thenReturn(10L);

        // when
        couponIssueListener.receive(message);

        // then
        verify(couponService, times(1)).issueMemberCoupon(1L, 10L);
    }

    @Test
    @DisplayName("비즈니스 예외 발생 시 로그만 찍고 정상 종료")
    void receive_BusinessException() {
        // given
        CouponIssueMessage message = mock(CouponIssueMessage.class);
        when(message.userId()).thenReturn(1L);
        when(message.couponId()).thenReturn(10L);

        doThrow(new CouponIssueException(CouponErrorCode.COUPON_OUT_OF_STOCK))
                .when(couponService).issueMemberCoupon(anyLong(), anyLong());

        // when & then
        assertDoesNotThrow(() -> couponIssueListener.receive(message));
    }

    @Test
    @DisplayName("시스템 예외 발생 시 예외 던짐")
    void receive_SystemException() {
        // given
        CouponIssueMessage message = mock(CouponIssueMessage.class);
        when(message.userId()).thenReturn(1L);
        when(message.couponId()).thenReturn(10L);

        doThrow(new RuntimeException("DB 에러"))
                .when(couponService).issueMemberCoupon(anyLong(), anyLong());

        // when & then
        assertThrows(RuntimeException.class, () -> couponIssueListener.receive(message));
    }
}