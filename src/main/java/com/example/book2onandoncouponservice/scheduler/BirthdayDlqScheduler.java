package com.example.book2onandoncouponservice.scheduler;

import com.example.book2onandoncouponservice.messaging.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BirthdayDlqScheduler {
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(cron = "0 0 0 * * * ")
    public void birthdayDlq() {
        log.info("Birthday.dlq 처리 스케줄러 시작");

        while (true) {
            Object message = rabbitTemplate.receiveAndConvert(RabbitConfig.QUEUE_BIRTHDAY_DLQ);

            if (message == null) {
                log.info("DLQ가 비어있습니다. 스케줄러 종료");
                break;
            }

            Long userId = (Long) message;
            log.info("원본 큐로 복구 userId: {}", userId);

            rabbitTemplate.convertAndSend(
                    RabbitConfig.EXCHANGE,
                    RabbitConfig.ROUTING_KEY_BIRTHDAY,
                    userId
            );
        }
    }

}
