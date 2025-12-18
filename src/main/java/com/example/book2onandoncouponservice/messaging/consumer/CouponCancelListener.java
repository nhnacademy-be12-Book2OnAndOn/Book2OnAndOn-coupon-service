package com.example.book2onandoncouponservice.messaging.consumer;

import com.example.book2onandoncouponservice.config.RabbitConfig;
import com.example.book2onandoncouponservice.exception.CouponIssueException;
import com.example.book2onandoncouponservice.exception.CouponNotFoundException;
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
    public void receive(String orderNumber) {

        try {
            log.info("쿠폰 롤백 시도 -> orderNumber: {}", orderNumber);
            memberCouponService.cancelMemberCoupon(orderNumber);
        } catch (CouponIssueException | CouponNotFoundException e) {
            log.info("발급 불가(비즈니스 예외) 재시도 하지 않음. reason:{}", e.getMessage());
        } catch (Exception e) {
            log.error("쿠폰 롤백 실패 orderNumber: {}", orderNumber);
            throw e;
        }
    }
}
