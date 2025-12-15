package com.example.book2onandoncouponservice.messaging.consumer;

import com.example.book2onandoncouponservice.client.DoorayHookClient;
import com.example.book2onandoncouponservice.config.RabbitConfig;
import com.example.book2onandoncouponservice.dooray.DoorayMessagePayload;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponCancelDlqListener {
    private final RabbitTemplate rabbitTemplate;
    private final DoorayHookClient doorayHookClient;
    private static final String X_DEATH_HEADER = "x-death";

    @Value("${dooray.service.id}")
    private String serviceId;

    @Value("${dooray.bot.id}")
    private String botId;

    @Value("${dooray.bot.botToken}")
    private String botToken;

    @RabbitListener(queues = RabbitConfig.QUEUE_CANCEL_DLQ)
    public void cancelCouponDlq(Message message) {
        try {
            long retryCount = getRetryCount(message);
            String reason = getErrorReason(message);

            Long orderId = (Long) rabbitTemplate.getMessageConverter().fromMessage(message);

            if (retryCount >= 3) {

                log.error("취소 롤백 최종 실패. 알림 발송. orderId={}, count={}, reason={}", orderId, retryCount, reason);

                sendDoorayAlert(String.valueOf(orderId), (int) retryCount, reason);

            } else {

                log.info("취소 롤백 재시도({}). orderId={}", retryCount, orderId);

                rabbitTemplate.convertAndSend(
                        RabbitConfig.ORDER_EXCHANGE,
                        RabbitConfig.ROUTING_KEY_CANCEL,
                        orderId
                );
            }
        } catch (Exception e) {
            log.error("Cancel DLQ 처리 중 예외 발생", e);
        }
    }

    // x-death 헤더에서 count(횟수) 추출
    private long getRetryCount(Message message) {
        Map<String, Object> headers = message.getMessageProperties().getHeaders();
        if (headers.containsKey(X_DEATH_HEADER)) {
            List<Map<String, Object>> deaths = (List<Map<String, Object>>) headers.get("x-death");
            if (!deaths.isEmpty()) {
                // RabbitMQ 버전에 따라 Long 또는 Integer일 수 있음
                Object count = deaths.get(0).get("count");
                return count instanceof Long ? (Long) count : ((Integer) count).longValue();
            }
        }
        return 0L;
    }

    // 에러 원인 추출
    private String getErrorReason(Message message) {
        Map<String, Object> headers = message.getMessageProperties().getHeaders();
        if (headers.containsKey(X_DEATH_HEADER)) {
            List<Map<String, Object>> deaths = (List<Map<String, Object>>) headers.get("x-death");
            if (!deaths.isEmpty()) {
                return String.valueOf(deaths.get(0).get("reason"));
            }
        }
        return "Unknown";
    }

    private void sendDoorayAlert(String failedMessageBody, int retryCount, String errorReason) {
        try {
            DoorayMessagePayload payload = DoorayMessagePayload.builder()
                    .botName("Coupon-Service-Alarm")
                    .text("[긴급] 쿠폰 롤백 실패 (DLQ)")
                    .attachments(Collections.singletonList(
                            DoorayMessagePayload.Attachment.builder()
                                    .title("최대 재시도 횟수(" + retryCount + "회) 초과")
                                    .text("실패 원인: " + errorReason + "\n\n" +
                                            "메시지 내용:\n" + failedMessageBody)
                                    .color("red")
                                    .build()
                    ))
                    .build();

            doorayHookClient.sendMessage(serviceId, botId, botToken, payload);
            log.info("Dooray 알림 전송 완료");
        } catch (Exception e) {
            log.error("Dooray 알림 전송 실패", e);
        }
    }
}
