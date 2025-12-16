package com.example.book2onandoncouponservice.scheduler;

import com.example.book2onandoncouponservice.client.DoorayHookClient;
import com.example.book2onandoncouponservice.config.RabbitConfig;
import com.example.book2onandoncouponservice.dooray.DoorayMessagePayload;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BirthdayDlqScheduler {
    private final RabbitTemplate rabbitTemplate;
    private final MessageConverter messageConverter;
    private final DoorayHookClient doorayHookClient;
    private static final String X_DEATH_HEADER = "x-death";

    @Value("${dooray.service.id}")
    private String serviceId;

    @Value("${dooray.bot.id}")
    private String botId;

    @Value("${dooray.bot.botToken}")
    private String botToken;

    @Scheduled(cron = "0 0 0 * * * ")
    @SchedulerLock(
            name = "birth_coupon_task",
            lockAtLeastFor = "30s",
            lockAtMostFor = "10m"
    )
    public void birthdayDlq() {
        log.info("Birthday.dlq 처리 스케줄러 시작");

        while (true) {
            Message message = rabbitTemplate.receive(RabbitConfig.QUEUE_BIRTHDAY_DLQ);

            if (message == null) {
                log.info("DLQ가 비어있습니다. 스케줄러 종료");
                break;
            }

            try {
                long retryCount = getRetryCount(message);
                String reason = getErrorReason(message);

                Object payload = messageConverter.fromMessage(message);
                Long userId = (Long) payload;

                if (retryCount >= 2) {
                    log.error("최대 재시도 횟수 초과. 알림 발송 및 폐기. userId={}, count={}", userId, retryCount);
                    sendDoorayAlert(String.valueOf(userId), (int) retryCount, reason);
                } else {
                    log.info("재시도 횟수({}). 원본 큐로 복구 userId: {}", retryCount, userId);

                    rabbitTemplate.convertAndSend(
                            RabbitConfig.USER_EXCHANGE,
                            RabbitConfig.ROUTING_KEY_BIRTHDAY,
                            userId
                    );
                }
            } catch (Exception e) {
                log.error("Birthday Coupon DLQ 처리 중 예외 발생", e);
            }
        }
    }

    // x-death 헤더에서 count(횟수) 추출
    private long getRetryCount(Message message) {
        Map<String, Object> headers = message.getMessageProperties().getHeaders();
        if (headers.containsKey(X_DEATH_HEADER)) {
            List<Map<String, Object>> deaths = (List<Map<String, Object>>) headers.get(X_DEATH_HEADER);
            if (!deaths.isEmpty()) {
                // RabbitMQ 버전에 따라 Long 또는 Integer일 수 있음
                Object count = deaths.get(0).get("count");
                return count instanceof Long l ? l : ((Integer) count).longValue();
            }
        }
        return 0L;
    }

    // 에러 원인 추출
    private String getErrorReason(Message message) {
        Map<String, Object> headers = message.getMessageProperties().getHeaders();
        if (headers.containsKey(X_DEATH_HEADER)) {
            List<Map<String, Object>> deaths = (List<Map<String, Object>>) headers.get(X_DEATH_HEADER);
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
                    .text("[긴급] 생일 쿠폰 발급 실패 (DLQ)")
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
