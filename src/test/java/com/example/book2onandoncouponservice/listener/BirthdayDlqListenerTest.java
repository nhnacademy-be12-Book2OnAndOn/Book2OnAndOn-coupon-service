package com.example.book2onandoncouponservice.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.book2onandoncouponservice.handler.DlqErrorHandler;
import com.example.book2onandoncouponservice.messaging.consumer.BirthdayDlqListener;
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
class BirthdayDlqListenerTest {

    @InjectMocks
    private BirthdayDlqListener listener;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private DlqErrorHandler dlqErrorHandler;

    @Mock
    private MessageConverter messageConverter;

    @Test
    @DisplayName("생일 쿠폰 DLQ 처리 성공")
    void birthdayDlq_Success() {
        // given
        Message message = mock(Message.class);
        Long userId = 999L;
        String reason = "Birthday Error";

        given(dlqErrorHandler.getErrorReason(any(Message.class))).willReturn(reason);
        given(rabbitTemplate.getMessageConverter()).willReturn(messageConverter);
        given(messageConverter.fromMessage(any(Message.class))).willReturn(userId);

        // when
        listener.birthdayDlq(message);

        // then
        verify(dlqErrorHandler).sendDoorayAlert(
                "[긴급] 생일 쿠폰 발급 실패 (DLQ)",
                String.valueOf(userId),
                reason
        );
    }

    @Test
    @DisplayName("생일 쿠폰 DLQ 처리 중 예외 발생")
    void birthdayDlq_Exception() {
        // given
        Message message = mock(Message.class);
        given(rabbitTemplate.getMessageConverter()).willThrow(new RuntimeException("Converter Error"));

        // when
        listener.birthdayDlq(message);

        // then
        verify(dlqErrorHandler, never()).sendDoorayAlert(anyString(), anyString(), anyString());
    }
}