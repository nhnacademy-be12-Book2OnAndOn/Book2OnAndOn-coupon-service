package com.example.book2onandoncouponservice.scheduler;

import com.example.book2onandoncouponservice.messaging.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WelcomeDlqScheduler {
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(cron = "0 0 0 * * * ")
    @SchedulerLock(
            name = "welcome_coupon_task",
            lockAtLeastFor = "30s",
            lockAtMostFor = "10m"
    )
    public void welcomeDlq() {
        log.info("welcome.dlq 처리 스케줄러 시작");

        while (true) {
            Object message = rabbitTemplate.receiveAndConvert(RabbitConfig.QUEUE_WELCOME_DLQ);

            if (message == null) {
                log.info("DLQ가 비어있습니다. 스케줄러 종료");
                break;
            }

            Long userId = (Long) message;
            log.info("원본 큐로 복구 userId: {}", userId);

            rabbitTemplate.convertAndSend(
                    RabbitConfig.USER_EXCHANGE,
                    RabbitConfig.ROUTING_KEY_WELCOME,
                    userId
            );
        }
    }
}
