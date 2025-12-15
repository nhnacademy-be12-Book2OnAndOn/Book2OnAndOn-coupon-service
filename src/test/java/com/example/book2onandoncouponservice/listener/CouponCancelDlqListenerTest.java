package com.example.book2onandoncouponservice.listener;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.book2onandoncouponservice.client.DoorayHookClient;
import com.example.book2onandoncouponservice.config.RabbitConfig;
import com.example.book2onandoncouponservice.dooray.DoorayMessagePayload;
import com.example.book2onandoncouponservice.messaging.consumer.CouponCancelDlqListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CouponCancelDlqListenerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private DoorayHookClient doorayHookClient;
    @Mock
    private MessageConverter messageConverter;

    @InjectMocks
    private CouponCancelDlqListener couponCancelDlqListener;

    @BeforeEach
    void setUp() {
        // @Value 필드 강제 주입
        ReflectionTestUtils.setField(couponCancelDlqListener, "serviceId", "test-service");
        ReflectionTestUtils.setField(couponCancelDlqListener, "botId", "test-bot");
        ReflectionTestUtils.setField(couponCancelDlqListener, "botToken", "test-token");
    }

    @Test
    @DisplayName("재시도 횟수 3회 미만(2회) -> 원본 큐로 복구")
    void cancelCouponDlq_Retry() {
        // given
        Message message = createMessageWithRetryCount(2L);
        Long orderId = 12345L;

        given(rabbitTemplate.getMessageConverter()).willReturn(messageConverter);
        given(messageConverter.fromMessage(message)).willReturn(orderId);

        // when
        couponCancelDlqListener.cancelCouponDlq(message);

        // then
        verify(rabbitTemplate).convertAndSend(
                RabbitConfig.ORDER_EXCHANGE,
                RabbitConfig.ROUTING_KEY_CANCEL,
                orderId
        );
        verify(doorayHookClient, never()).sendMessage(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("재시도 횟수 3회 이상(3회) -> 알림 발송 및 폐기")
    void cancelCouponDlq_Alert() {
        // given
        Message message = createMessageWithRetryCount(3L);
        Long orderId = 12345L;

        given(rabbitTemplate.getMessageConverter()).willReturn(messageConverter);
        given(messageConverter.fromMessage(message)).willReturn(orderId);

        // when
        couponCancelDlqListener.cancelCouponDlq(message);

        // then
        verify(doorayHookClient).sendMessage(
                eq("test-service"), eq("test-bot"), eq("test-token"), any(DoorayMessagePayload.class)
        );
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    @DisplayName("처리 중 예외 발생 시 로그 찍고 종료 (Swallow Exception)")
    void cancelCouponDlq_Exception() {
        // given
        Message message = mock(Message.class);
        // MessageProperties 접근 시 예외 유발
        given(message.getMessageProperties()).willThrow(new RuntimeException("Parsing Error"));

        // when & then
        assertDoesNotThrow(() -> couponCancelDlqListener.cancelCouponDlq(message));
    }

    // Helper: x-death 헤더 생성
    private Message createMessageWithRetryCount(long count) {
        Map<String, Object> deathHeader = new HashMap<>();
        deathHeader.put("count", count);
        deathHeader.put("reason", "rejected");

        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setHeader("x-death", List.of(deathHeader));

        return new Message("body".getBytes(), messageProperties);
    }
}