package com.example.book2onandoncouponservice.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.book2onandoncouponservice.handler.DlqErrorHandler;
import com.example.book2onandoncouponservice.messaging.CouponIssueMessage;
import com.example.book2onandoncouponservice.messaging.consumer.CouponIssueDlqListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class CouponIssueDlqListenerTest {

    @InjectMocks
    private CouponIssueDlqListener listener;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private DlqErrorHandler dlqErrorHandler;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private MessageConverter messageConverter;

    @Test
    @DisplayName("쿠폰 발급 DLQ 처리 성공: 재고 복구 및 알림 발송")
    void issueCouponDlq_Success() {
        // given
        Message message = mock(Message.class);
        Long userId = 10L;
        Long couponId = 200L;
        CouponIssueMessage payload = new CouponIssueMessage(userId, couponId); // record 가정, class면 생성자 확인
        String reason = "Stock Error";

        given(dlqErrorHandler.getErrorReason(any(Message.class))).willReturn(reason);
        given(rabbitTemplate.getMessageConverter()).willReturn(messageConverter);
        given(messageConverter.fromMessage(any(Message.class))).willReturn(payload);

        // Redis Mocking
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        listener.issueCouponDlq(message);

        // then
        // 1. 재고 복구 확인 (키 형식 일치 여부 중요)
        verify(valueOperations).increment("coupon:" + couponId + "stock:");

        // 2. 알림 발송 확인
        verify(dlqErrorHandler).sendDoorayAlert(
                "[긴급] 쿠폰 발급 실패 (DLQ)",
                payload.toString(),
                reason
        );
    }

    @Test
    @DisplayName("쿠폰 발급 DLQ 처리 중 예외 발생")
    void issueCouponDlq_Exception() {
        // given
        Message message = mock(Message.class);
        given(dlqErrorHandler.getErrorReason(any(Message.class))).willThrow(new RuntimeException("Fail"));

        // when
        listener.issueCouponDlq(message);

        // then
        verify(redisTemplate, never()).opsForValue();
        verify(dlqErrorHandler, never()).sendDoorayAlert(anyString(), anyString(), anyString());
    }
}