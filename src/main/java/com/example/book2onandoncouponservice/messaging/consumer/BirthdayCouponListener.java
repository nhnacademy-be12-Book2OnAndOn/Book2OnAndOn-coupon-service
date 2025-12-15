package com.example.book2onandoncouponservice.messaging.consumer;

import com.example.book2onandoncouponservice.config.RabbitConfig;
import com.example.book2onandoncouponservice.exception.CouponIssueException;
import com.example.book2onandoncouponservice.exception.CouponNotFoundException;
import com.example.book2onandoncouponservice.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BirthdayCouponListener {
    private final CouponService couponService;

    @RabbitListener(queues = RabbitConfig.QUEUE_BIRTHDAY)
    public void receive(Long userId) {

        try {
            log.info("생일 쿠폰 발급 시도 -> userId: {}", userId);
            couponService.issueBirthdayCoupon(userId);
        } catch (CouponNotFoundException | CouponIssueException e) {
            log.info("발급 불가(비즈니스 예외) 재시도 하지 않음. reason:{}", e.getMessage());
        } catch (Exception e) {
            log.error("생일 쿠폰 발급 실패 userId: {}", userId);
            throw e;
        }
    }
}
