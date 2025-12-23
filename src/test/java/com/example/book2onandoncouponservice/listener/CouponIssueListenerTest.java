package com.example.book2onandoncouponservice.listener;

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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class CouponIssueListenerTest {

    @Mock
    private CouponService couponService;

    @InjectMocks
    private CouponIssueListener couponIssueListener;

    @Mock
    StringRedisTemplate redisTemplate;

    @Mock
    ValueOperations<String, String> valueOperations;

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
    @DisplayName("비즈니스 예외 발생 시 Redis 재고 복구(increment) 호출 확인")
    void receive_BusinessException() {
        // given
        Long userId = 1L;
        Long couponId = 10L;
        // [중요] 실제 코드에 적힌 키 포맷과 정확히 일치해야 합니다. (오타 주의!)
        String expectedKey = "coupon:" + couponId + "stock:";

        CouponIssueMessage message = mock(CouponIssueMessage.class);
        when(message.userId()).thenReturn(userId);
        when(message.couponId()).thenReturn(couponId);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        doThrow(new CouponIssueException(CouponErrorCode.COUPON_OUT_OF_STOCK))
                .when(couponService).issueMemberCoupon(anyLong(), anyLong());

        // when
        couponIssueListener.receive(message);

        // then
        verify(valueOperations, times(1)).increment(expectedKey);
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