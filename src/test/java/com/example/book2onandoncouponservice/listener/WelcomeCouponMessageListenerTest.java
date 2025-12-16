package com.example.book2onandoncouponservice.listener;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.book2onandoncouponservice.messaging.consumer.WelcomeCouponMessageListener;
import com.example.book2onandoncouponservice.service.CouponService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WelcomeCouponMessageListenerTest {

    @Mock
    private CouponService couponService;

    @InjectMocks
    private WelcomeCouponMessageListener welcomeCouponMessageListener;

    @Test
    @DisplayName("웰컴 쿠폰 발급 성공")
    void receive_Success() {
        // given
        Long userId = 1L;

        // when
        welcomeCouponMessageListener.receive(userId);

        // then
        verify(couponService, times(1)).issueWelcomeCoupon(userId);
    }

    @Test
    @DisplayName("예외 발생 시 다시 던짐")
    void receive_Exception() {
        // given
        Long userId = 1L;
        doThrow(new RuntimeException("에러"))
                .when(couponService).issueWelcomeCoupon(userId);

        // when & then
        assertThrows(RuntimeException.class, () -> welcomeCouponMessageListener.receive(userId));
    }
}