package com.example.book2onandoncouponservice.scheduler;

import com.example.book2onandoncouponservice.repository.MemberCouponRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpireCouponBulkScheduler {
    private final MemberCouponRepository memberCouponRepository;

    @Scheduled(cron = "0 0 0 * * *")
    @SchedulerLock(
            name = "expired_coupon_task",
            lockAtLeastFor = "30s",
            lockAtMostFor = "10m"
    )
    @Transactional
    public void expiredCoupons() {

        log.info("쿠폰 만료처리 스케줄러 시작: {}", LocalDateTime.now());

        int count = memberCouponRepository.bulkExpireCoupons(LocalDateTime.now());
        log.info("{}개의 쿠폰 만료처리 완료", count);
    }
}
