package com.example.book2onandoncouponservice.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.book2onandoncouponservice.entity.Coupon;
import com.example.book2onandoncouponservice.entity.CouponPolicy;
import com.example.book2onandoncouponservice.exception.CouponIssueException;
import com.example.book2onandoncouponservice.messaging.CouponIssueMessage;
import com.example.book2onandoncouponservice.repository.CouponRepository;
import com.example.book2onandoncouponservice.repository.MemberCouponRepository;
import com.example.book2onandoncouponservice.service.impl.CouponIssueService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class CouponIssueServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private CouponRepository couponRepository;
    @Mock
    private MemberCouponRepository memberCouponRepository;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private CouponIssueService couponIssueService;

    private final Long userId = 1L;
    private final Long couponId = 100L;
    private final String stockKey = "coupon:100stock:";

    private Coupon setupBaseCouponMock(Integer remainingQuantity, boolean isIssuable) {
        Coupon coupon = mock(Coupon.class);
        CouponPolicy policy = mock(CouponPolicy.class);

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));
        when(coupon.getCouponPolicy()).thenReturn(policy);
        when(policy.isIssuable()).thenReturn(isIssuable);

        if (isIssuable) {
            lenient().when(coupon.getCouponRemainingQuantity()).thenReturn(remainingQuantity);
            lenient().when(memberCouponRepository.existsByUserIdAndCoupon_CouponId(userId, couponId)).thenReturn(false);
        }

        return coupon;
    }

    @Test
    @DisplayName("쿠폰 발급 요청 성공 - 유한 재고")
    void issueRequest_Success() {
        // given
        setupBaseCouponMock(100, true);
        given(redisTemplate.opsForValue()).willReturn(valueOperations); // 필요한 시점에만 정의
        given(valueOperations.decrement(stockKey)).willReturn(99L);

        // when
        couponIssueService.issueRequest(userId, couponId);

        // then
        verify(valueOperations).decrement(stockKey);
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(CouponIssueMessage.class));
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 정책상 발급 불가")
    void issueRequest_Fail_PolicyNotIssuable() {
        // given
        setupBaseCouponMock(100, false); // isIssuable = false

        // when & then
        assertThrows(CouponIssueException.class, () -> couponIssueService.issueRequest(userId, couponId));

        // Redis 연산은 실행되지 않으므로 redisTemplate 관련 stubbing이 없어도 에러가 나지 않음
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 이미 발급된 쿠폰")
    void issueRequest_Fail_AlreadyIssued() {
        // given
        Coupon coupon = mock(Coupon.class);
        CouponPolicy policy = mock(CouponPolicy.class);
        given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));
        given(coupon.getCouponPolicy()).willReturn(policy);
        given(policy.isIssuable()).willReturn(true);
        given(memberCouponRepository.existsByUserIdAndCoupon_CouponId(userId, couponId)).willReturn(true);

        // when & then
        assertThrows(CouponIssueException.class, () -> couponIssueService.issueRequest(userId, couponId));
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - RabbitMQ 에러 시 재고 복구")
    void issueRequest_Fail_RabbitMQError() {
        // given
        setupBaseCouponMock(100, true);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.decrement(stockKey)).willReturn(99L);
        willThrow(new AmqpException("Error")).given(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(CouponIssueMessage.class));

        // when & then
        assertThrows(CouponIssueException.class, () -> couponIssueService.issueRequest(userId, couponId));
        verify(valueOperations).increment(stockKey);
    }
}