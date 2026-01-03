package com.example.book2onandoncouponservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.book2onandoncouponservice.entity.Coupon;
import com.example.book2onandoncouponservice.entity.CouponPolicy;
import com.example.book2onandoncouponservice.exception.CouponIssueException;
import com.example.book2onandoncouponservice.messaging.CouponIssueMessage;
import com.example.book2onandoncouponservice.repository.CouponRepository;
import com.example.book2onandoncouponservice.service.impl.CouponIssueService;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.SetOperations;
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
    private CouponService couponService; // 무제한 쿠폰 로직용

    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private SetOperations<String, String> setOperations;

    @InjectMocks
    private CouponIssueService couponIssueService;

    private final Long userId = 1L;
    private final Long couponId = 100L;

    private final String stockKey = "coupon:100stock:";
    private final String issueKey = "coupon:100users:";

    private void setupCoupon(Integer remainingQuantity, boolean isIssuable) {
        Coupon coupon = mock(Coupon.class);
        CouponPolicy policy = mock(CouponPolicy.class);

        given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));
        given(coupon.getCouponPolicy()).willReturn(policy);
        given(policy.isIssuable()).willReturn(isIssuable);

        // isLimited 판단 로직
        if (isIssuable) {
            given(coupon.getCouponRemainingQuantity()).willReturn(remainingQuantity);
        }
    }

    private void setupRedisMocks() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
    }

    @Test
    @DisplayName("무제한 쿠폰: 동기 발급 성공 (Redis/MQ 사용 안 함)")
    void issueRequest_Unlimited_Success() {
        // given
        // remainingQuantity가 null이면 무제한
        setupCoupon(null, true);

        // when
        boolean result = couponIssueService.issueRequest(userId, couponId);

        // then
        assertThat(result).isTrue(); // 즉시 발급 완료
        verify(couponService).issueMemberCoupon(userId, couponId);

        // Redis나 MQ는 호출되지 않아야 함
        verify(redisTemplate, never()).opsForValue();
        verify(redisTemplate, never()).opsForSet();
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(CouponIssueMessage.class));
    }

    @Test
    @DisplayName("선착순 쿠폰: 발급 요청 접수 성공 (Redis Set -> Stock -> MQ)")
    void issueRequest_Limited_Success() {
        // given
        setupCoupon(100, true);
        setupRedisMocks();

        // 1. Set 중복 검사 (성공: 1 반환)
        given(setOperations.add(issueKey, String.valueOf(userId))).willReturn(1L);
        // 2. 재고 차감 (성공: 99 반환)
        given(valueOperations.decrement(stockKey)).willReturn(99L);

        // when
        boolean result = couponIssueService.issueRequest(userId, couponId);

        // then
        assertThat(result).isFalse(); // 비동기 접수 완료

        // 순서 검증: TTL 설정 확인
        verify(redisTemplate).expire(eq(issueKey), any(Duration.class));
        // MQ 전송 확인
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(CouponIssueMessage.class));
    }

    @Test
    @DisplayName("선착순 쿠폰: 중복 요청 시 재고 차감 없이 예외 발생")
    void issueRequest_Limited_Fail_Duplicate() {
        // given
        setupCoupon(100, true);
        setupRedisMocks();

        // Redis Set에 이미 존재함 (0 반환)
        given(setOperations.add(issueKey, String.valueOf(userId))).willReturn(0L);

        // when & then
        assertThrows(CouponIssueException.class, () -> couponIssueService.issueRequest(userId, couponId));

        // 검증: 재고 차감(decrement)은 절대로 호출되면 안 됨 (순서 보장)
        verify(valueOperations, never()).decrement(anyString());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(CouponIssueMessage.class));
    }

    @Test
    @DisplayName("선착순 쿠폰: 재고 소진 시 Set 롤백 및 예외 발생")
    void issueRequest_Limited_Fail_OutOfStock() {
        // given
        setupCoupon(100, true);
        setupRedisMocks();

        given(setOperations.add(issueKey, String.valueOf(userId))).willReturn(1L);
        // 재고가 0보다 작아짐
        given(valueOperations.decrement(stockKey)).willReturn(-1L);

        // when & then
        assertThrows(CouponIssueException.class, () -> couponIssueService.issueRequest(userId, couponId));

        // 롤백 검증: Set에서 유저 제거
        verify(setOperations).remove(issueKey, String.valueOf(userId));
        verify(valueOperations).increment(stockKey);

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(CouponIssueMessage.class));
    }

    @Test
    @DisplayName("선착순 쿠폰: MQ 전송 실패 시 전체 롤백 (Set + Stock)")
    void issueRequest_Limited_Fail_RabbitMQError() {
        // given
        setupCoupon(100, true);
        setupRedisMocks();

        given(setOperations.add(issueKey, String.valueOf(userId))).willReturn(1L);
        given(valueOperations.decrement(stockKey)).willReturn(99L);

        // MQ 전송 시 예외 발생
        willThrow(new AmqpException("MQ Error")).given(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(CouponIssueMessage.class));

        // when & then
        assertThrows(CouponIssueException.class, () -> couponIssueService.issueRequest(userId, couponId));

        verify(valueOperations).increment(stockKey); // 재고 복구
        verify(setOperations).remove(issueKey, String.valueOf(userId)); // 이력 삭제
    }

    @Test
    @DisplayName("공통: 발급 불가능한 정책 예외")
    void issueRequest_Fail_PolicyNotIssuable() {
        // given
        setupCoupon(100, false); // isIssuable = false

        // when & then
        assertThrows(CouponIssueException.class, () -> couponIssueService.issueRequest(userId, couponId));

        verify(redisTemplate, never()).opsForValue();
    }
}