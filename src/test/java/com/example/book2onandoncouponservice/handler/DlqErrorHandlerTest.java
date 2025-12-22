package com.example.book2onandoncouponservice.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.book2onandoncouponservice.client.DoorayHookClient;
import com.example.book2onandoncouponservice.dooray.DoorayMessagePayload;
import java.util.ArrayList;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DlqErrorHandlerTest {

    @Mock
    private DoorayHookClient doorayHookClient;

    @InjectMocks
    private DlqErrorHandler dlqErrorHandler;

    @BeforeEach
    void setUp() {
        // @Value 필드 주입 시뮬레이션
        ReflectionTestUtils.setField(dlqErrorHandler, "serviceId", "test-service-id");
        ReflectionTestUtils.setField(dlqErrorHandler, "botId", "test-bot-id");
        ReflectionTestUtils.setField(dlqErrorHandler, "botToken", "test-bot-token");
    }

    @Test
    @DisplayName("에러 사유 추출: x-death 헤더가 있고 내용이 존재할 때 사유를 반환해야 한다")
    void getErrorReason_WithXDeathHeader() {
        // given
        Message message = mock(Message.class);
        MessageProperties messageProperties = mock(MessageProperties.class);

        Map<String, Object> headers = new HashMap<>();
        List<Map<String, Object>> xDeathList = new ArrayList<>();
        Map<String, Object> xDeathEntry = new HashMap<>();
        xDeathEntry.put("reason", "rejected");
        xDeathList.add(xDeathEntry);

        headers.put("x-death", xDeathList);

        when(message.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getHeaders()).thenReturn(headers);

        // when
        String reason = dlqErrorHandler.getErrorReason(message);

        // then
        assertThat(reason).isEqualTo("rejected");
    }

    @Test
    @DisplayName("에러 사유 추출: x-death 헤더가 있지만 리스트가 비어있으면 Unknown을 반환해야 한다")
    void getErrorReason_WithEmptyXDeathHeader() {
        // given
        Message message = mock(Message.class);
        MessageProperties messageProperties = mock(MessageProperties.class);

        Map<String, Object> headers = new HashMap<>();
        List<Map<String, Object>> xDeathList = Collections.emptyList();

        headers.put("x-death", xDeathList);

        when(message.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getHeaders()).thenReturn(headers);

        // when
        String reason = dlqErrorHandler.getErrorReason(message);

        // then
        assertThat(reason).isEqualTo("Unknown");
    }

    @Test
    @DisplayName("에러 사유 추출: x-death 헤더가 없으면 Unknown을 반환해야 한다")
    void getErrorReason_NoXDeathHeader() {
        // given
        Message message = mock(Message.class);
        MessageProperties messageProperties = mock(MessageProperties.class);
        Map<String, Object> headers = new HashMap<>(); // 빈 헤더

        when(message.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getHeaders()).thenReturn(headers);

        // when
        String reason = dlqErrorHandler.getErrorReason(message);

        // then
        assertThat(reason).isEqualTo("Unknown");
    }

    @Test
    @DisplayName("두레이 알림 전송: 성공 케이스")
    void sendDoorayAlert_Success() {
        // given
        String text = "Error Alert";
        String failedMessageBody = "{\"id\":1}";
        String errorReason = "rejected";

        // when
        dlqErrorHandler.sendDoorayAlert(text, failedMessageBody, errorReason);

        // then
        verify(doorayHookClient, times(1)).sendMessage(
                eq("test-service-id"),
                eq("test-bot-id"),
                eq("test-bot-token"),
                any(DoorayMessagePayload.class)
        );
    }

    @Test
    @DisplayName("두레이 알림 전송: 예외 발생 시 로그를 남기고 에러를 전파하지 않아야 한다")
    void sendDoorayAlert_Exception() {
        // given
        String text = "Error Alert";
        String failedMessageBody = "body";
        String errorReason = "reason";

        doThrow(new RuntimeException("API Error"))
                .when(doorayHookClient)
                .sendMessage(any(), any(), any(), any());

        // when & then
        // 예외가 발생해도 try-catch로 잡히므로 테스트가 실패하지 않아야 함
        dlqErrorHandler.sendDoorayAlert(text, failedMessageBody, errorReason);

        verify(doorayHookClient, times(1)).sendMessage(any(), any(), any(), any());
    }
}