package com.example.book2onandoncouponservice.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpireCouponBatchScheduler {
    private final JobLauncher jobLauncher;
    private final Job couponExpirejob;

    @Scheduled(cron = "0 0 0 * * *")
    @SchedulerLock(
            name = "expired_coupon_task",
            lockAtLeastFor = "30s",
            lockAtMostFor = "10m"
    )
    public void runExpireJob() {
        try {
            log.info("쿠폰 만료 배치 시작");

            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(couponExpirejob, jobParameters);
        } catch (JobInstanceAlreadyCompleteException | JobExecutionAlreadyRunningException |
                 JobParametersInvalidException | JobRestartException e) {
            log.error("쿠폰 만료 배치 실행 중 오류 발생", e);
        }
    }
}
