package com.example.book2onandoncouponservice.messaging.consumer;

import com.example.book2onandoncouponservice.config.RabbitConfig;
import com.example.book2onandoncouponservice.exception.CouponIssueException;
import com.example.book2onandoncouponservice.exception.CouponNotFoundException;
import com.example.book2onandoncouponservice.messaging.CouponIssueMessage;
import com.example.book2onandoncouponservice.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponIssueListener {
    private final CouponService couponService;
    private final StringRedisTemplate redisTemplate;

    @RabbitListener(queues = RabbitConfig.QUEUE_ISSUE)
    public void receive(CouponIssueMessage issueMessage) {

        try {
            log.debug("쿠폰 발급 메시지 수신. userId = {}, couponId = {}", issueMessage.userId(), issueMessage.couponId());
            couponService.issueMemberCoupon(issueMessage.userId(), issueMessage.couponId());
        } catch (CouponIssueException | CouponNotFoundException e) {
            log.error("발급 불가(비즈니스 예외) 재시도 하지 않음. reason:{}", e.getMessage());

            String stockKey = "coupon:" + issueMessage.couponId() + "stock:";
            redisTemplate.opsForValue().increment(stockKey);

        } catch (Exception e) {
            log.error("쿠폰 지급 중 에러 발생 재시도. userId = {}, couponId = {}", issueMessage.userId(), issueMessage.couponId(),
                    e);
            throw e;
        }
    }
}
