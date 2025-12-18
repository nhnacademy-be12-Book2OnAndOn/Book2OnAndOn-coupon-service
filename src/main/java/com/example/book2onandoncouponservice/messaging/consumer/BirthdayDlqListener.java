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
public class BirthdayDlqListener {

    private final RabbitTemplate rabbitTemplate;
    private final DlqErrorHandler dlqErrorHandler;


    @RabbitListener(queues = RabbitConfig.QUEUE_BIRTHDAY_DLQ)
    public void birthdayDlq(Message message) {

        try {
            String reason = dlqErrorHandler.getErrorReason(message);

            Object payload = rabbitTemplate.getMessageConverter().fromMessage(message);
            Long userId = (Long) payload;

            log.error("알림 발송 대상 userId={}, reason={}", userId, reason);
            String text = "[긴급] 생일 쿠폰 발급 실패 (DLQ)";
            dlqErrorHandler.sendDoorayAlert(text, String.valueOf(userId), reason);

        } catch (Exception e) {
            log.error("Birthday Coupon DLQ 처리 중 예외 발생", e);
        }
    }
}