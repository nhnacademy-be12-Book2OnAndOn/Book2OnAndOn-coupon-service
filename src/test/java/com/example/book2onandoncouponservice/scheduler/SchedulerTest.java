package com.example.book2onandoncouponservice.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.book2onandoncouponservice.client.DoorayHookClient;
import com.example.book2onandoncouponservice.config.RabbitConfig;
import com.example.book2onandoncouponservice.dooray.DoorayMessagePayload;
import com.example.book2onandoncouponservice.repository.MemberCouponRepository;
import java.time.LocalDateTime;
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
class SchedulerTest {

    @Mock
    RabbitTemplate rabbitTemplate;
    @Mock
    MemberCouponRepository memberCouponRepository;
    @Mock
    MessageConverter messageConverter;
    @Mock
    DoorayHookClient doorayHookClient;

    @InjectMocks
    WelcomeDlqScheduler welcomeDlqScheduler;
    @InjectMocks
    BirthdayDlqScheduler birthdayDlqScheduler;
    @InjectMocks
    ExpireCouponBulkScheduler expireCouponBulkScheduler;

    @BeforeEach
    void setUp() {
        // @Value 필드에 값 주입 (WelcomeDlqScheduler)
        ReflectionTestUtils.setField(welcomeDlqScheduler, "serviceId", "test-service");
        ReflectionTestUtils.setField(welcomeDlqScheduler, "botId", "test-bot");
        ReflectionTestUtils.setField(welcomeDlqScheduler, "botToken", "test-token");

        // @Value 필드에 값 주입 (BirthdayDlqScheduler)
        ReflectionTestUtils.setField(birthdayDlqScheduler, "serviceId", "test-service");
        ReflectionTestUtils.setField(birthdayDlqScheduler, "botId", "test-bot");
        ReflectionTestUtils.setField(birthdayDlqScheduler, "botToken", "test-token");
    }

    @Test
    @DisplayName("Welcome DLQ - 재시도 횟수 미만(1회)일 때 원본 큐로 복구")
    void welcomeDlq_Requeue_WhenRetryCountIsLow() {
        // given
        Message message = createMessageWithRetryCount(1L);

        // Mocking: 큐에서 메시지를 꺼내옴
        given(rabbitTemplate.receive(RabbitConfig.QUEUE_BIRTHDAY_DLQ))
                .willReturn(message)
                .willReturn(null);

        given(messageConverter.fromMessage(message)).willReturn(100L);

        // when
        welcomeDlqScheduler.welcomeDlq();

        // then
        // 원본 큐로 복구되었는지 확인
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitConfig.USER_EXCHANGE),
                eq(RabbitConfig.ROUTING_KEY_WELCOME),
                eq(100L)
        );
        // 알림은 발송되지 않아야 함
        verify(doorayHookClient, never()).sendMessage(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Welcome DLQ - 재시도 횟수 초과(2회)일 때 알림 발송 및 폐기")
    void welcomeDlq_Alert_WhenRetryCountIsHigh() {
        // given
        // x-death 헤더 (count = 2)
        Message message = createMessageWithRetryCount(2L);

        given(rabbitTemplate.receive(RabbitConfig.QUEUE_BIRTHDAY_DLQ))
                .willReturn(message)
                .willReturn(null);

        given(messageConverter.fromMessage(message)).willReturn(100L);

        // when
        welcomeDlqScheduler.welcomeDlq();

        // then
        // 알림이 발송되었는지 확인
        verify(doorayHookClient, times(1)).sendMessage(
                eq("test-service"), eq("test-bot"), eq("test-token"), any(DoorayMessagePayload.class)
        );
        // 원본 큐로 복구되지 않아야 함
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    @DisplayName("Birthday DLQ - 재시도 횟수 미만(1회)일 때 원본 큐로 복구")
    void birthdayDlq_Requeue() {
        // given
        Message message = createMessageWithRetryCount(1L);

        given(rabbitTemplate.receive(RabbitConfig.QUEUE_BIRTHDAY_DLQ))
                .willReturn(message)
                .willReturn(null);

        given(messageConverter.fromMessage(message)).willReturn(200L);

        // when
        birthdayDlqScheduler.birthdayDlq();

        // then
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitConfig.USER_EXCHANGE),
                eq(RabbitConfig.ROUTING_KEY_BIRTHDAY),
                eq(200L)
        );
    }

    @Test
    @DisplayName("만료 쿠폰 벌크 업데이트 스케줄러")
    void expireCoupon_Success() {
        // given
        given(memberCouponRepository.bulkExpireCoupons(any(LocalDateTime.class))).willReturn(10);

        // when
        expireCouponBulkScheduler.expiredCoupons();

        // then
        verify(memberCouponRepository).bulkExpireCoupons(any(LocalDateTime.class));
    }

    // --- Helper Method ---
    private Message createMessageWithRetryCount(long count) {
        Map<String, Object> deathHeader = new HashMap<>();
        deathHeader.put("count", count);
        deathHeader.put("reason", "rejected");

        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setHeader("x-death", List.of(deathHeader));

        return new Message("test-body".getBytes(), messageProperties);
    }
}