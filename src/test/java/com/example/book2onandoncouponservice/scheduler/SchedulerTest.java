package com.example.book2onandoncouponservice.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.book2onandoncouponservice.messaging.config.RabbitConfig;
import com.example.book2onandoncouponservice.repository.MemberCouponRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class SchedulerTest {

    @Mock
    RabbitTemplate rabbitTemplate;
    @Mock
    MemberCouponRepository memberCouponRepository;

    @InjectMocks
    WelcomeDlqScheduler welcomeDlqScheduler;
    @InjectMocks
    BirthdayDlqScheduler birthdayDlqScheduler;
    @InjectMocks
    CancelDlqScheduler cancelDlqScheduler;
    @InjectMocks
    ExpireCouponBulkScheduler expireCouponBulkScheduler;

    @Test
    @DisplayName("Welcome DLQ 스케줄러 - 메시지 복구 성공")
    void welcomeDlq_Success() {
        // given
        given(rabbitTemplate.receiveAndConvert(RabbitConfig.QUEUE_WELCOME_DLQ))
                .willReturn(100L)
                .willReturn(null);

        // when
        welcomeDlqScheduler.welcomeDlq();

        // then
        verify(rabbitTemplate, times(1)).convertAndSend(
                RabbitConfig.USER_EXCHANGE,
                RabbitConfig.ROUTING_KEY_WELCOME,
                100L
        );
    }

    @Test
    @DisplayName("Birthday DLQ 스케줄러 - 메시지 복구 성공")
    void birthdayDlq_Success() {
        // given
        given(rabbitTemplate.receiveAndConvert(RabbitConfig.QUEUE_BIRTHDAY_DLQ))
                .willReturn(200L)
                .willReturn(null);

        // when
        birthdayDlqScheduler.birthdayDlq();

        // then
        verify(rabbitTemplate, times(1)).convertAndSend(
                RabbitConfig.USER_EXCHANGE,
                RabbitConfig.ROUTING_KEY_BIRTHDAY,
                200L
        );
    }

    @Test
    @DisplayName("Cancel DLQ 스케줄러 - 메시지 복구 성공")
    void cancelDlq_Success() {
        // given
        given(rabbitTemplate.receiveAndConvert(RabbitConfig.QUEUE_CANCEL_DLQ))
                .willReturn(12345L)
                .willReturn(null);

        // when
        cancelDlqScheduler.cancelCouponDlq();

        // then
        verify(rabbitTemplate, times(1)).convertAndSend(
                RabbitConfig.ORDER_EXCHANGE,
                RabbitConfig.ROUTING_KEY_CANCEL,
                12345L
        );
    }

    @Test
    @DisplayName("만료 쿠폰 벌크 업데이트 스케줄러")
    void expireCoupon_Success() {
        // given
        given(memberCouponRepository.bulkExpireCoupons(any(LocalDateTime.class))).willReturn(10);

        // when
        expireCouponBulkScheduler.expiredCoupons();

        // then
        verify(memberCouponRepository).bulkExpireCoupons(any(LocalDateTime.class));
    }
}