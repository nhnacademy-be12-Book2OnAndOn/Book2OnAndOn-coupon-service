package com.example.book2onandoncouponservice.config;

import com.example.book2onandoncouponservice.entity.MemberCoupon;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CouponExpireJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;

    private static final int CHUNK_SIZE = 1000;

    @Bean
    public Job couponExpireJob() {
        return new JobBuilder("couponExpireJob", jobRepository)
                .start(couponExpireStep())
                .build();
    }

    @Bean
    public Step couponExpireStep() {
        return new StepBuilder("couponExpireStep", jobRepository)
                .<MemberCoupon, MemberCoupon>chunk(CHUNK_SIZE, transactionManager)
                .reader(couponExpireReader())
                .processor(couponExpireProcessor())
                .writer(couponExpireWriter())
                .faultTolerant()
                .retry(Exception.class)
                .retryLimit(3)
                .build();
    }

    @Bean
    @StepScope
    public JpaCursorItemReader<MemberCoupon> couponExpireReader() {
        return new JpaCursorItemReaderBuilder<MemberCoupon>()
                .name("couponExpireReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString(
                        "SELECT mc FROM MemberCoupon mc WHERE mc.memberCouponEndDate < :now AND mc.memberCouponStatus = 'NOT_USED'")
                .parameterValues(Map.of("now", LocalDateTime.now()))
                .build();
    }

    @Bean
    public ItemProcessor<MemberCoupon, MemberCoupon> couponExpireProcessor() {
        return memberCoupon -> {
            memberCoupon.expire(); // MemberCoupon 엔티티 내부의 expire 로직 수행
            return memberCoupon;
        };
    }

    @Bean
    public JpaItemWriter<MemberCoupon> couponExpireWriter() {
        return new JpaItemWriterBuilder<MemberCoupon>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }
}