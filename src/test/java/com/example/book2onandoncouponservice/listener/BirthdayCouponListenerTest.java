package com.example.book2onandoncouponservice.listener;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.book2onandoncouponservice.exception.CouponErrorCode;
import com.example.book2onandoncouponservice.exception.CouponIssueException;
import com.example.book2onandoncouponservice.messaging.consumer.BirthdayCouponListener;
import com.example.book2onandoncouponservice.service.CouponService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BirthdayCouponListenerTest {

    @Mock
    private CouponService couponService;

    @InjectMocks
    private BirthdayCouponListener birthdayCouponListener;

    @Test
    @DisplayName("생일 쿠폰 발급 성공")
    void receive_Success() {
        // given
        Long userId = 1L;

        // when
        birthdayCouponListener.receive(userId);

        // then
        verify(couponService, times(1)).issueBirthdayCoupon(userId);
    }

    @Test
    @DisplayName("비즈니스 예외 발생 시 재시도 하지 않음 (예외 무시)")
    void receive_BusinessException() {
        // given
        Long userId = 1L;
        doThrow(new CouponIssueException(CouponErrorCode.COUPON_ALREADY_ISSUED))
                .when(couponService).issueBirthdayCoupon(userId);

        // when & then
        assertDoesNotThrow(() -> birthdayCouponListener.receive(userId));
        verify(couponService, times(1)).issueBirthdayCoupon(userId);
    }

    @Test
    @DisplayName("시스템 예외 발생 시 예외를 던짐 (재시도 유발)")
    void receive_SystemException() {
        // given
        Long userId = 1L;
        doThrow(new RuntimeException("DB 연결 오류"))
                .when(couponService).issueBirthdayCoupon(userId);

        // when & then
        assertThrows(RuntimeException.class, () -> birthdayCouponListener.receive(userId));
    }
}