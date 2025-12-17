package com.example.book2onandoncouponservice.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;

@ExtendWith(MockitoExtension.class)
class ExpireCouponBulkSchedulerTest {

    @InjectMocks
    private ExpireCouponBatchScheduler scheduler;

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private Job couponExpirejob;

    @Test
    @DisplayName("쿠폰 만료 배치 Job 실행 성공")
    void runExpireJob_Success() throws Exception {
        // when
        scheduler.runExpireJob();

        // then
        verify(jobLauncher, times(1)).run(eq(couponExpirejob), any(JobParameters.class));
    }

    @Test
    @DisplayName("배치 실행 중 예외 발생 시 로그를 남기고 중단되지 않음")
    void runExpireJob_Exception() throws Exception {
        // give
        doThrow(new JobExecutionAlreadyRunningException("Job is already running"))
                .when(jobLauncher).run(any(Job.class), any(JobParameters.class));

        // when
        scheduler.runExpireJob();

        // then
        verify(jobLauncher, times(1)).run(eq(couponExpirejob), any(JobParameters.class));
    }
}