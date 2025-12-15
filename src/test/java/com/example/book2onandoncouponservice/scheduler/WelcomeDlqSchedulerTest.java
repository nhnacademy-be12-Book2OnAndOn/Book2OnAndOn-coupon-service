package com.example.book2onandoncouponservice.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.book2onandoncouponservice.client.DoorayHookClient;
import com.example.book2onandoncouponservice.config.RabbitConfig;
import com.example.book2onandoncouponservice.dooray.DoorayMessagePayload;
import java.util.Collections;
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
class WelcomeDlqSchedulerTest {

    @InjectMocks
    private WelcomeDlqScheduler scheduler;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private MessageConverter messageConverter;

    @Mock
    private DoorayHookClient doorayHookClient;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "serviceId", "test-service-id");
        ReflectionTestUtils.setField(scheduler, "botId", "test-bot-id");
        ReflectionTestUtils.setField(scheduler, "botToken", "test-bot-token");
    }

    private Message createMessage(List<Map<String, Object>> xDeathHeader) {
        MessageProperties properties = new MessageProperties();
        if (xDeathHeader != null) {
            properties.getHeaders().put("x-death", xDeathHeader);
        }
        return new Message("test-body".getBytes(), properties);
    }

    @Test
    @DisplayName("DLQ가 비어있으면 스케줄러 종료")
    void welcomeDlq_EmptyQueue() {
        // given
        given(rabbitTemplate.receive(anyString())).willReturn(null);

        // when
        scheduler.welcomeDlq();

        // then
        verify(rabbitTemplate, times(1)).receive(anyString());
        verify(messageConverter, never()).fromMessage(any());
    }

    @Test
    @DisplayName("재시도 횟수 2회 미만: 원본 큐로 복구 (Long 타입 Count)")
    void welcomeDlq_RetryLessThan2_Requeue() {
        // given
        Map<String, Object> xDeath = new HashMap<>();
        xDeath.put("count", 1L);
        xDeath.put("reason", "expired");
        Message message = createMessage(Collections.singletonList(xDeath));

        given(rabbitTemplate.receive(anyString()))
                .willReturn(message) // 첫 번째 호출: 메시지 반환
                .willReturn(null);   // 두 번째 호출: 종료
        given(messageConverter.fromMessage(message)).willReturn(123L);

        // when
        scheduler.welcomeDlq();

        // then
        // 원본 큐로 다시 보내는지 확인
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConfig.USER_EXCHANGE),
                eq(RabbitConfig.ROUTING_KEY_WELCOME),
                eq(123L)
        );
        // 알림은 안 보냄
        verify(doorayHookClient, never()).sendMessage(any(), any(), any(), any());
    }

    @Test
    @DisplayName("재시도 횟수 2회 미만: 원본 큐로 복구 (Integer 타입 Count)")
    void welcomeDlq_RetryLessThan2_IntegerCount() {
        // given
        Map<String, Object> xDeath = new HashMap<>();
        xDeath.put("count", 1); // Integer
        xDeath.put("reason", "expired");
        Message message = createMessage(Collections.singletonList(xDeath));

        given(rabbitTemplate.receive(anyString()))
                .willReturn(message)
                .willReturn(null);
        given(messageConverter.fromMessage(message)).willReturn(123L);

        // when
        scheduler.welcomeDlq();

        // then
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), eq(123L));
    }

    @Test
    @DisplayName("재시도 횟수 2회 이상: Dooray 알림 발송")
    void welcomeDlq_RetryExceeded_SendAlert() {
        // given
        Map<String, Object> xDeath = new HashMap<>();
        xDeath.put("count", 2L);
        xDeath.put("reason", "rejected");
        Message message = createMessage(Collections.singletonList(xDeath));

        given(rabbitTemplate.receive(anyString()))
                .willReturn(message)
                .willReturn(null);
        given(messageConverter.fromMessage(message)).willReturn(123L);

        // when
        scheduler.welcomeDlq();

        // then
        // 원본 큐 전송 X
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Long.class));
        // 알림 전송 O
        verify(doorayHookClient).sendMessage(
                eq("test-service-id"),
                eq("test-bot-id"),
                eq("test-bot-token"),
                any(DoorayMessagePayload.class)
        );
    }

    @Test
    @DisplayName("헤더 파싱 실패(헤더 없음): 재시도 횟수 0으로 처리 -> 복구")
    void welcomeDlq_NoHeader() {
        // given
        Message message = createMessage(null); // 헤더 없음

        given(rabbitTemplate.receive(anyString()))
                .willReturn(message)
                .willReturn(null);
        given(messageConverter.fromMessage(message)).willReturn(123L);

        // when
        scheduler.welcomeDlq();

        // then
        // retryCount = 0 이므로 복구 로직 실행
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), eq(123L));
    }

    @Test
    @DisplayName("헤더 파싱 실패(리스트 비어있음): 재시도 횟수 0으로 처리")
    void welcomeDlq_EmptyHeaderList() {
        // given
        Message message = createMessage(Collections.emptyList());

        given(rabbitTemplate.receive(anyString()))
                .willReturn(message)
                .willReturn(null);
        given(messageConverter.fromMessage(message)).willReturn(123L);

        // when
        scheduler.welcomeDlq();

        // then
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), eq(123L));
    }

//    @Test
//    @DisplayName("처리 중 예외 발생 시 로그 출력하고 계속 진행(종료되지 않음)")
//    void welcomeDlq_ExceptionHandling() {
//        // given
//        Message message1 = createMessage(null);
//        Message message2 = createMessage(null);
//
//        given(rabbitTemplate.receive(anyString()))
//                .willReturn(message1) // 첫 번째 메시지: 예외 발생 유도
//                .willReturn(message2) // 두 번째 메시지: 정상 처리
//                .willReturn(null);
//
//        // 첫 번째 메시지 처리 시 예외 발생
//        willThrow(new RuntimeException("Converter Error"))
//                .given(messageConverter).fromMessage(message1);
//
//        // 두 번째 메시지는 정상 리턴
//        given(messageConverter.fromMessage(message2)).willReturn(456L);
//
//        // when
//        scheduler.welcomeDlq();
//
//        // then
//        // 첫 번째는 실패해서 convertAndSend 호출 안됨 (예외 catch)
//        // 두 번째는 성공해서 호출됨
//        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), 456L);
//    }

    @Test
    @DisplayName("Dooray 알림 전송 실패 시 예외 처리")
    void welcomeDlq_DoorayError() {
        // given
        Map<String, Object> xDeath = new HashMap<>();
        xDeath.put("count", 5L);
        Message message = createMessage(Collections.singletonList(xDeath));

        given(rabbitTemplate.receive(anyString()))
                .willReturn(message)
                .willReturn(null);
        given(messageConverter.fromMessage(message)).willReturn(123L);

        // Dooray Client가 예외를 던짐
        willThrow(new RuntimeException("Dooray API Error"))
                .given(doorayHookClient).sendMessage(any(), any(), any(), any());

        // when
        scheduler.welcomeDlq();

        // then
        // 스케줄러가 죽지 않고 정상 종료(null 수신)까지 가는지 확인
        verify(doorayHookClient).sendMessage(any(), any(), any(), any());
    }
}