package com.example.book2onandoncouponservice.messaging.consumer;

import com.example.book2onandoncouponservice.messaging.config.RabbitConfig;
import com.example.book2onandoncouponservice.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WelcomeCouponMessageListener {
    private final CouponService couponService;

    @RabbitListener(queues = RabbitConfig.QUEUE_WELCOME)
    public void receive(Long userId) {
        log.info("RabbitMQ -> 회원가입 이벤트 수신. userId={}", userId);

        try {
            couponService.issueWelcomeCoupon(userId);
        } catch (Exception e) {
            log.error("웰컴 쿠폰 발급 실패 userId = {}", userId);
            throw e;
        }
    }
}
