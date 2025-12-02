package com.example.book2onandoncouponservice.messaging.consumer;

import com.example.book2onandoncouponservice.messaging.config.RabbitConfig;
import com.example.book2onandoncouponservice.service.MemberCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponCancelListener {
    private final MemberCouponService memberCouponService;

    @RabbitListener(queues = RabbitConfig.QUEUE_CANCEL)
    public void receive(Long orderId) {

        try {
            log.info("쿠폰 롤백 시도 -> orderId: {}", orderId);
            memberCouponService.cancelMemberCoupon(orderId);
        } catch (Exception e) {
            log.error("생일 쿠폰 발급 실패 orderId: {}", orderId);
            throw e;
        }
    }
}
