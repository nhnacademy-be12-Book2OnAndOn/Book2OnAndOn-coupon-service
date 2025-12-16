package com.example.book2onandoncouponservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.book2onandoncouponservice.config.RabbitConfig;
import com.example.book2onandoncouponservice.exception.CouponErrorCode;
import com.example.book2onandoncouponservice.exception.CouponIssueException;
import com.example.book2onandoncouponservice.messaging.CouponIssueMessage;
import com.example.book2onandoncouponservice.service.impl.CouponIssueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class CouponIssueServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private CouponIssueService couponIssueService;

    // RedisTemplate.opsForValue()가 Mock 객체(valueOperations)를 반환하도록 설정
    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
    }

    @Test
    @DisplayName("쿠폰 발급 요청 성공 - 재고 차감 및 큐 전송 완료")
    void issueRequest_Success() {
        // given
        Long userId = 1L;
        Long couponId = 100L;
        String expectedKey = "coupon:" + couponId + "stock:";

        // 재고 감소 요청 시 10(양수) 반환 가정
        given(valueOperations.decrement(expectedKey)).willReturn(10L);

        // when
        couponIssueService.issueRequest(userId, couponId);

        // then
        // 1. Redis 감소 호출 확인
        verify(valueOperations).decrement(expectedKey);

        // 2. RabbitMQ 전송 호출 확인
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConfig.COUPON_EXCHANGE),
                eq(RabbitConfig.ROUTING_KEY_ISSUE),
                any(CouponIssueMessage.class)
        );

        // 3. 재고 복구(increment)는 호출되지 않아야 함
        verify(valueOperations, never()).increment(anyString());
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 재고 소진 (Redis 값이 음수)")
    void issueRequest_Fail_OutOfStock() {
        // given
        Long userId = 1L;
        Long couponId = 100L;
        String expectedKey = "coupon:" + couponId + "stock:";

        // 재고 감소 요청 시 -1(음수) 반환 가정 -> 재고 없음
        given(valueOperations.decrement(expectedKey)).willReturn(-1L);

        // when & then
        CouponIssueException exception = assertThrows(CouponIssueException.class, () ->
                couponIssueService.issueRequest(userId, couponId)
        );

        assertEquals(CouponErrorCode.COUPON_OUT_OF_STOCK, exception.getErrorCode());

        // RabbitMQ 전송은 일어나지 않아야 함
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - RabbitMQ 전송 오류 시 재고 복구(Rollback)")
    void issueRequest_Fail_RabbitMQError() {
        // given
        Long userId = 1L;
        Long couponId = 100L;
        String expectedKey = "coupon:" + couponId + "stock:";

        // 1. 재고 차감은 성공했다고 가정
        given(valueOperations.decrement(expectedKey)).willReturn(99L);

        // 2. 하지만 RabbitMQ 전송 시 예외 발생
        willThrow(new AmqpException("RabbitMQ Connection Error"))
                .given(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(CouponIssueMessage.class));

        // when & then
        CouponIssueException exception = assertThrows(CouponIssueException.class, () ->
                couponIssueService.issueRequest(userId, couponId)
        );

        assertEquals(CouponErrorCode.FAIL_TO_ISSUE_COUPON, exception.getErrorCode());

        // [핵심 검증] 예외 발생 시 Redis 재고를 다시 증가(복구)시켰는지 확인
        verify(valueOperations).increment(expectedKey);
    }
}