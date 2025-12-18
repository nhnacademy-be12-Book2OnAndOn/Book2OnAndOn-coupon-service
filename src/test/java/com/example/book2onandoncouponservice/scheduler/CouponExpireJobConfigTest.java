package com.example.book2onandoncouponservice.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.book2onandoncouponservice.entity.Coupon;
import com.example.book2onandoncouponservice.entity.CouponPolicy;
import com.example.book2onandoncouponservice.entity.CouponPolicyDiscountType;
import com.example.book2onandoncouponservice.entity.CouponPolicyType;
import com.example.book2onandoncouponservice.entity.MemberCoupon;
import com.example.book2onandoncouponservice.entity.MemberCouponStatus;
import com.example.book2onandoncouponservice.repository.CouponPolicyRepository;
import com.example.book2onandoncouponservice.repository.CouponRepository;
import com.example.book2onandoncouponservice.repository.MemberCouponRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
class CouponExpireJobConfigTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private MemberCouponRepository memberCouponRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponPolicyRepository couponPolicyRepository;

    @Autowired
    private Job couponExpireJob;

    @AfterEach
    void tearDown() {
        memberCouponRepository.deleteAll();
        couponRepository.deleteAll();
        couponPolicyRepository.deleteAll();
    }

    @Test
    @DisplayName("만료일이 지난 쿠폰은 상태가 EXPIRED로 변경되어야 한다")
    void couponExpireJob_Success() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();

        // 1. CouponPolicy 생성 및 저장
        CouponPolicy policy = CouponPolicy.builder()
                .couponPolicyName("테스트 정책")
                .couponPolicyType(CouponPolicyType.CUSTOM)
                .couponPolicyDiscountType(CouponPolicyDiscountType.FIXED)
                .couponDiscountValue(1000)
                .minPrice(10000)
                .maxPrice(50000)
                .build();

        // [수정] save는 한 번만 호출하고, 반환된 객체를 받아서 사용합니다.
        CouponPolicy savedPolicy = couponPolicyRepository.save(policy);

        // 2. Coupon 생성 (savedPolicy 사용)
        Coupon coupon = Coupon.builder()
                .couponPolicy(savedPolicy)
                // .couponId(1L) -> [수정] ID는 DB 자동 생성에 맡기므로 제거 (충돌 방지)
                .couponRemainingQuantity(100)
                .build();

        // [수정] 역시 save는 한 번만 호출합니다.
        Coupon savedCoupon = couponRepository.save(coupon);

        // 3. MemberCoupon 생성 (savedCoupon 사용)
        MemberCoupon expiredCoupon = MemberCoupon.builder()
                .coupon(savedCoupon)
                .userId(1L)
                .memberCouponEndDate(now.minusDays(1))
                .memberCouponStatus(MemberCouponStatus.NOT_USED)
                .memberCouponIssuedDate(now.minusDays(10))
                .build();

        MemberCoupon validCoupon = MemberCoupon.builder()
                .coupon(savedCoupon)
                .userId(2L)
                .memberCouponEndDate(now.plusDays(1))
                .memberCouponStatus(MemberCouponStatus.NOT_USED)
                .memberCouponIssuedDate(now.minusDays(1))
                .build();

        MemberCoupon usedCoupon = MemberCoupon.builder()
                .coupon(savedCoupon)
                .userId(3L)
                .memberCouponEndDate(now.minusDays(1))
                .memberCouponStatus(MemberCouponStatus.USED)
                .memberCouponIssuedDate(now.minusDays(10))
                .build();

        memberCouponRepository.save(expiredCoupon);
        memberCouponRepository.save(validCoupon);
        memberCouponRepository.save(usedCoupon);

        // when
        jobLauncherTestUtils.setJob(couponExpireJob);

        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // 트랜잭션이 끝난 상태에서 DB를 다시 조회하여 확인
        MemberCoupon updatedExpiredCoupon = memberCouponRepository.findById(expiredCoupon.getMemberCouponId())
                .orElseThrow();
        assertThat(updatedExpiredCoupon.getMemberCouponStatus()).isEqualTo(MemberCouponStatus.EXPIRED);

        MemberCoupon updatedValidCoupon = memberCouponRepository.findById(validCoupon.getMemberCouponId())
                .orElseThrow();
        assertThat(updatedValidCoupon.getMemberCouponStatus()).isEqualTo(MemberCouponStatus.NOT_USED);

        MemberCoupon updatedUsedCoupon = memberCouponRepository.findById(usedCoupon.getMemberCouponId())
                .orElseThrow();
        assertThat(updatedUsedCoupon.getMemberCouponStatus()).isEqualTo(MemberCouponStatus.USED);
    }
}