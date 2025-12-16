package com.example.book2onandoncouponservice.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.book2onandoncouponservice.client.DoorayHookClient;
import com.example.book2onandoncouponservice.config.RabbitConfig;
import com.example.book2onandoncouponservice.dooray.DoorayMessagePayload;
import java.util.Collections;
import java.util.HashMap;
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
class BirthdayDlqSchedulerTest {

    @InjectMocks
    private BirthdayDlqScheduler scheduler;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private MessageConverter messageConverter;

    @Mock
    private DoorayHookClient doorayHookClient;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "serviceId", "service-id");
        ReflectionTestUtils.setField(scheduler, "botId", "bot-id");
        ReflectionTestUtils.setField(scheduler, "botToken", "bot-token");
    }

    private Message createMessage(long retryCount) {
        Map<String, Object> xDeath = new HashMap<>();
        xDeath.put("count", retryCount);
        xDeath.put("reason", "expired");

        MessageProperties properties = new MessageProperties();
        properties.getHeaders().put("x-death", Collections.singletonList(xDeath));

        return new Message("body".getBytes(), properties);
    }

    @Test
    @DisplayName("DLQ 메시지 없음")
    void birthdayDlq_Empty() {
        given(rabbitTemplate.receive(anyString())).willReturn(null);
        scheduler.birthdayDlq();
        verify(messageConverter, never()).fromMessage(any());
    }

    @Test
    @DisplayName("재시도 횟수 초과 -> 알림 발송")
    void birthdayDlq_Alert() {
        Message message = createMessage(3L);
        given(rabbitTemplate.receive(anyString())).willReturn(message).willReturn(null);
        given(messageConverter.fromMessage(message)).willReturn(999L);

        scheduler.birthdayDlq();

        verify(doorayHookClient).sendMessage(
                eq("service-id"),
                eq("bot-id"),
                eq("bot-token"),
                any(DoorayMessagePayload.class)
        );
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Long.class));
    }

    @Test
    @DisplayName("재시도 횟수 미만 -> 원본 큐 복구")
    void birthdayDlq_Requeue() {
        Message message = createMessage(1L);
        given(rabbitTemplate.receive(anyString())).willReturn(message).willReturn(null);
        given(messageConverter.fromMessage(message)).willReturn(888L);

        scheduler.birthdayDlq();

        verify(rabbitTemplate).convertAndSend(
                RabbitConfig.USER_EXCHANGE,
                RabbitConfig.ROUTING_KEY_BIRTHDAY,
                888L
        );
        verify(doorayHookClient, never()).sendMessage(any(), any(), any(), any());
    }

    @Test
    @DisplayName("x-death 헤더에서 count가 Integer일 경우 처리")
    void birthdayDlq_IntegerCount() {
        Map<String, Object> xDeath = new HashMap<>();
        xDeath.put("count", (int) 1); // Integer
        xDeath.put("reason", "expired");

        MessageProperties properties = new MessageProperties();
        properties.getHeaders().put("x-death", Collections.singletonList(xDeath));
        Message message = new Message("body".getBytes(), properties);

        given(rabbitTemplate.receive(anyString())).willReturn(message).willReturn(null);
        given(messageConverter.fromMessage(message)).willReturn(777L);

        scheduler.birthdayDlq();

        // Integer 1 -> Long 1L로 변환되어 retryCount < 2 분기 진입 -> Requeue
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), eq(777L));
    }
}