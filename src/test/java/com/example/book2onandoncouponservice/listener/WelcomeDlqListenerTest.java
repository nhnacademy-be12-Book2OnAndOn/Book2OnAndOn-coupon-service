package com.example.book2onandoncouponservice.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.book2onandoncouponservice.handler.DlqErrorHandler;
import com.example.book2onandoncouponservice.messaging.consumer.WelcomeDlqListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;

@ExtendWith(MockitoExtension.class)
class WelcomeDlqListenerTest {

    @InjectMocks
    private WelcomeDlqListener listener;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private DlqErrorHandler dlqErrorHandler;

    @Mock
    private MessageConverter messageConverter;

    @Test
    @DisplayName("웰컴 쿠폰 DLQ 처리 성공")
    void welcomeDlq_Success() {
        // given
        Message message = mock(Message.class);
        Long userId = 777L;
        String reason = "Welcome Error";

        given(dlqErrorHandler.getErrorReason(any(Message.class))).willReturn(reason);
        given(rabbitTemplate.getMessageConverter()).willReturn(messageConverter);
        given(messageConverter.fromMessage(any(Message.class))).willReturn(userId);

        // when
        listener.welcomeDlq(message);

        // then
        verify(dlqErrorHandler).sendDoorayAlert(
                "[긴급] 웰컴쿠폰 발급 실패 (DLQ)",
                String.valueOf(userId),
                reason
        );
    }

    @Test
    @DisplayName("웰컴 쿠폰 DLQ 처리 중 예외 발생")
    void welcomeDlq_Exception() {
        // given
        Message message = mock(Message.class);
        given(rabbitTemplate.getMessageConverter()).willThrow(new RuntimeException("Error"));

        // when
        listener.welcomeDlq(message);

        // then
        verify(dlqErrorHandler, never()).sendDoorayAlert(anyString(), anyString(), anyString());
    }
}