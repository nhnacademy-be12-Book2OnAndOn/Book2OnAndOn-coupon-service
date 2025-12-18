package com.example.book2onandoncouponservice.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.book2onandoncouponservice.handler.DlqErrorHandler;
import com.example.book2onandoncouponservice.messaging.consumer.CouponCancelDlqListener;
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
class CouponCancelDlqListenerTest {

    @InjectMocks
    private CouponCancelDlqListener listener;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private DlqErrorHandler dlqErrorHandler;

    @Mock
    private MessageConverter messageConverter;

    @Test
    @DisplayName("취소 DLQ 처리 성공: 알림 발송 확인")
    void cancelCouponDlq_Success() {
        // given
        Message message = mock(Message.class);
        String orderNumber = "12345L";
        String reason = "Test Error Reason";

        given(dlqErrorHandler.getErrorReason(any(Message.class))).willReturn(reason);
        given(rabbitTemplate.getMessageConverter()).willReturn(messageConverter);
        given(messageConverter.fromMessage(any(Message.class))).willReturn(orderNumber);

        // when
        listener.cancelCouponDlq(message);

        // then
        verify(dlqErrorHandler).sendDoorayAlert(
                "[긴급] 쿠폰 롤백 실패 (DLQ)",
                String.valueOf(orderNumber),
                reason
        );
    }

    @Test
    @DisplayName("취소 DLQ 처리 중 예외 발생: 로그 남기고 종료 (예외 던지지 않음)")
    void cancelCouponDlq_Exception() {
        // given
        Message message = mock(Message.class);
        given(dlqErrorHandler.getErrorReason(any(Message.class))).willThrow(new RuntimeException("Error!"));

        // when
        listener.cancelCouponDlq(message);

        // then
        // 예외가 catch 블록에서 잡혔으므로 sendDoorayAlert는 호출되지 않아야 함
        verify(dlqErrorHandler, never()).sendDoorayAlert(anyString(), anyString(), anyString());
    }
}