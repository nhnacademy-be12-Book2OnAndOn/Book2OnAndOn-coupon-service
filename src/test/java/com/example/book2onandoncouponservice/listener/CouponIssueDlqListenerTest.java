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
import com.example.book2onandoncouponservice.messaging.CouponIssueMessage;
import com.example.book2onandoncouponservice.messaging.consumer.CouponIssueDlqListener;
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
class CouponIssueDlqListenerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private DoorayHookClient doorayHookClient;
    @Mock
    private MessageConverter messageConverter;

    @InjectMocks
    private CouponIssueDlqListener couponIssueDlqListener;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(couponIssueDlqListener, "serviceId", "test-service");
        ReflectionTestUtils.setField(couponIssueDlqListener, "botId", "test-bot");
        ReflectionTestUtils.setField(couponIssueDlqListener, "botToken", "test-token");
    }

    @Test
    @DisplayName("재시도 횟수 3회 미만 -> 재시도")
    void processIssueDlq_Retry() {
        // given
        Message message = createMessageWithRetryCount(1L);
        // DTO Mocking
        CouponIssueMessage dto = mock(CouponIssueMessage.class);

        given(rabbitTemplate.getMessageConverter()).willReturn(messageConverter);
        given(messageConverter.fromMessage(message)).willReturn(dto);

        // when
        couponIssueDlqListener.IssueCouponDlq(message);

        // then
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConfig.COUPON_EXCHANGE),
                eq(RabbitConfig.ROUTING_KEY_ISSUE),
                eq(dto)
        );
        verify(doorayHookClient, never()).sendMessage(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("재시도 횟수 3회 이상 -> 알림 발송")
    void processIssueDlq_Alert() {
        // given
        Message message = createMessageWithRetryCount(3L);
        CouponIssueMessage dto = mock(CouponIssueMessage.class);

        given(rabbitTemplate.getMessageConverter()).willReturn(messageConverter);
        given(messageConverter.fromMessage(message)).willReturn(dto);

        // when
        couponIssueDlqListener.IssueCouponDlq(message);

        // then
        verify(doorayHookClient).sendMessage(
                eq("test-service"), eq("test-bot"), eq("test-token"), any(DoorayMessagePayload.class)
        );
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    @DisplayName("예외 발생 시 안전하게 종료")
    void processIssueDlq_Exception() {
        // given
        Message message = mock(Message.class);
        given(message.getMessageProperties()).willThrow(new RuntimeException("Error"));

        // when & then
        assertDoesNotThrow(() -> couponIssueDlqListener.IssueCouponDlq(message));
    }

    // Helper
    private Message createMessageWithRetryCount(long count) {
        Map<String, Object> deathHeader = new HashMap<>();
        deathHeader.put("count", count);
        deathHeader.put("reason", "rejected");

        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setHeader("x-death", List.of(deathHeader));

        return new Message("body".getBytes(), messageProperties);
    }
}