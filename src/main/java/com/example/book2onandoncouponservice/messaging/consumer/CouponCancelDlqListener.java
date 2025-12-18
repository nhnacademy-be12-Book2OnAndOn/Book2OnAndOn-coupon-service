package com.example.book2onandoncouponservice.messaging.consumer;

import com.example.book2onandoncouponservice.config.RabbitConfig;
import com.example.book2onandoncouponservice.handler.DlqErrorHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponCancelDlqListener {
    private final RabbitTemplate rabbitTemplate;
    private final DlqErrorHandler dlqErrorHandler;

    @RabbitListener(queues = RabbitConfig.QUEUE_CANCEL_DLQ)
    public void cancelCouponDlq(Message message) {

        try {
            String reason = dlqErrorHandler.getErrorReason(message);

            String orderNumber = (String) rabbitTemplate.getMessageConverter().fromMessage(message);

            log.error("취소 롤백 최종 실패. 알림 발송. orderNumber={}, reason={}", orderNumber, reason);
            String text = "[긴급] 쿠폰 롤백 실패 (DLQ)";
            dlqErrorHandler.sendDoorayAlert(text, orderNumber, reason);

        } catch (Exception e) {
            log.error("Cancel DLQ 처리 중 예외 발생", e);
        }
    }
}
