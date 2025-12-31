package com.example.book2onandoncouponservice.service.impl;

import com.example.book2onandoncouponservice.config.RabbitConfig;
import com.example.book2onandoncouponservice.entity.Coupon;
import com.example.book2onandoncouponservice.exception.CouponErrorCode;
import com.example.book2onandoncouponservice.exception.CouponIssueException;
import com.example.book2onandoncouponservice.messaging.CouponIssueMessage;
import com.example.book2onandoncouponservice.repository.CouponRepository;
import com.example.book2onandoncouponservice.repository.MemberCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponIssueService {

    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final CouponRepository couponRepository;
    private final MemberCouponRepository memberCouponRepository;

    @Transactional(readOnly = true)
    public void issueRequest(Long userId, Long couponId) {

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CouponIssueException(CouponErrorCode.COUPON_NOT_FOUND));

        if (!coupon.getCouponPolicy().isIssuable()) {
            log.warn("발급 불가능한 정책. policyId={}, userId={}", coupon.getCouponPolicy().getCouponPolicyId(), userId);
            throw new CouponIssueException(CouponErrorCode.POLICY_NOT_ISSUABLE);
        }

        if (memberCouponRepository.existsByUserIdAndCoupon_CouponId(userId, couponId)) {
            log.warn("이미 발급된 쿠폰. userId={}, couponId={}", userId, couponId);
            throw new CouponIssueException(CouponErrorCode.COUPON_ALREADY_ISSUED);
        }

        String stockKey = "coupon:" + couponId + "stock:";
        boolean isLimited = coupon.getCouponRemainingQuantity() != null;

        if (isLimited) {
            Long stock = redisTemplate.opsForValue().decrement(stockKey);

            if (stock != null && stock < 0) {
                log.info("쿠폰 재고 소진. userId={}, couponId={}", userId, couponId);
                throw new CouponIssueException(CouponErrorCode.COUPON_OUT_OF_STOCK);
            }
        }

        try {
            CouponIssueMessage message = new CouponIssueMessage(userId, couponId);
            rabbitTemplate.convertAndSend(RabbitConfig.COUPON_EXCHANGE, RabbitConfig.ROUTING_KEY_ISSUE, message);

            log.info("쿠폰 발급 요청 접수 완료. userId={}, couponId={}", userId, couponId);

        } catch (Exception e) {
            if (isLimited) {
                redisTemplate.opsForValue().increment(stockKey);
            }
            log.error("메시지 큐 전송 에러. userId={}, couponId={}", userId, couponId, e);
            throw new CouponIssueException(CouponErrorCode.FAIL_TO_ISSUE_COUPON);
        }
    }
}
