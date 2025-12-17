package com.example.book2onandoncouponservice.messaging.consumer;

import com.example.book2onandoncouponservice.config.RabbitConfig;
import com.example.book2onandoncouponservice.handler.DlqErrorHandler;
import com.example.book2onandoncouponservice.messaging.CouponIssueMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponIssueDlqListener {
    private final RabbitTemplate rabbitTemplate;
    private final DlqErrorHandler dlqErrorHandler;
    private final StringRedisTemplate redisTemplate;


    @RabbitListener(queues = RabbitConfig.QUEUE_ISSUE_DLQ)
    public void issueCouponDlq(Message message) {
        try {
            String reason = dlqErrorHandler.getErrorReason(message);

            CouponIssueMessage payload =
                    (CouponIssueMessage) rabbitTemplate.getMessageConverter().fromMessage(message);

            // Redis 재고 복구
            String stockKey = "coupon:" + payload.couponId() + "stock:";
            redisTemplate.opsForValue().increment(stockKey);

            log.error(
                    "쿠폰 발급 최종 실패(DLQ). userId={}, couponId={}, reason={}",
                    payload.userId(),
                    payload.couponId(),
                    reason
            );

            String text = "[긴급] 쿠폰 발급 실패 (DLQ)";
            dlqErrorHandler.sendDoorayAlert(text, payload.toString(), reason);

        } catch (Exception e) {
            log.error("Issue DLQ 처리 중 예외 발생", e);
        }
    }
}
