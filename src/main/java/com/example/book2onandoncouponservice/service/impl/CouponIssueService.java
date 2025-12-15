package com.example.book2onandoncouponservice.service.impl;

import com.example.book2onandoncouponservice.config.RabbitConfig;
import com.example.book2onandoncouponservice.exception.CouponErrorCode;
import com.example.book2onandoncouponservice.exception.CouponIssueException;
import com.example.book2onandoncouponservice.messaging.CouponIssueMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponIssueService {

    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;

    public void issueRequest(Long userId, Long couponId) {

        String stockKey = "coupon:" + couponId + "stock:";

        Long stock = redisTemplate.opsForValue().decrement(stockKey);

        if (stock != null && stock < 0) {
            log.info("재고소진. userId={}, couponId={}", userId, couponId);

            throw new CouponIssueException(CouponErrorCode.COUPON_OUT_OF_STOCK);
        }

        try {
            CouponIssueMessage message = new CouponIssueMessage(userId, couponId);
            rabbitTemplate.convertAndSend(RabbitConfig.COUPON_EXCHANGE, RabbitConfig.QUEUE_ISSUE, message);

            log.info("쿠폰 발급 메시지 큐 전송. userId={}", userId);

        } catch (Exception e) {
            redisTemplate.opsForValue().increment(stockKey);
            log.error("메시지 전송 실패 쿠폰 재고 복구. userId={}", userId);
            throw new CouponIssueException(CouponErrorCode.FAIL_TO_ISSUE_COUPON);
        }
    }
}
