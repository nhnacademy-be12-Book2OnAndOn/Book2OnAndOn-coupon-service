package com.example.book2onandoncouponservice.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.book2onandoncouponservice.repository.MemberCouponRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpireCouponBulkSchedulerTest {

    @InjectMocks
    private ExpireCouponBulkScheduler scheduler;

    @Mock
    private MemberCouponRepository memberCouponRepository;

    @Test
    @DisplayName("쿠폰 만료 처리 스케줄러 실행 성공")
    void expiredCoupons_Success() {
        // given
        given(memberCouponRepository.bulkExpireCoupons(any(LocalDateTime.class)))
                .willReturn(10); // 10개 만료 처리됨

        // when
        scheduler.expiredCoupons();

        // then
        verify(memberCouponRepository).bulkExpireCoupons(any(LocalDateTime.class));
    }
}