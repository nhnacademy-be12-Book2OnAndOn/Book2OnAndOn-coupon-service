package com.example.book2onandoncouponservice.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.book2onandoncouponservice.entity.MemberCoupon;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
class CouponExpireJobConfigTest {

    @Mock
    private JobRepository jobRepository;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private EntityManagerFactory entityManagerFactory;

    @InjectMocks
    private CouponExpireJobConfig couponExpireJobConfig;

    @Test
    @DisplayName("Job Bean 생성 확인")
    void couponExpireJob() {
        Job job = couponExpireJobConfig.couponExpireJob();
        assertThat(job).isNotNull();
        assertThat(job.getName()).isEqualTo("couponExpireJob");
    }

    @Test
    @DisplayName("Step Bean 생성 확인")
    void couponExpireStep() {
        Step step = couponExpireJobConfig.couponExpireStep();
        assertThat(step).isNotNull();
        assertThat(step.getName()).isEqualTo("couponExpireStep");
    }

    @Test
    @DisplayName("Reader Bean 생성 확인")
    void couponExpireReader() {
        // Reader 생성 시 EntityManagerFactory가 사용됨
        JpaCursorItemReader<MemberCoupon> reader = couponExpireJobConfig.couponExpireReader();
        assertThat(reader).isNotNull();
    }

    @Test
    @DisplayName("Writer Bean 생성 확인")
    void couponExpireWriter() {
        JpaItemWriter<MemberCoupon> writer = couponExpireJobConfig.couponExpireWriter();
        assertThat(writer).isNotNull();
    }

    @Test
    @DisplayName("Processor 로직 테스트: expire 메서드가 호출되어야 한다")
    void couponExpireProcessor() throws Exception {
        // given
        ItemProcessor<MemberCoupon, MemberCoupon> processor = couponExpireJobConfig.couponExpireProcessor();
        MemberCoupon mockCoupon = mock(MemberCoupon.class);

        // when
        MemberCoupon result = processor.process(mockCoupon);

        // then
        assertThat(result).isEqualTo(mockCoupon);
        verify(mockCoupon, times(1)).expire();
    }
}