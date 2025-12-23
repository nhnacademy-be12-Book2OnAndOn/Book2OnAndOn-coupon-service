package com.example.book2onandoncouponservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import com.example.book2onandoncouponservice.dto.request.CouponCreateRequestDto;
import com.example.book2onandoncouponservice.dto.response.CouponResponseDto;
import com.example.book2onandoncouponservice.entity.Coupon;
import com.example.book2onandoncouponservice.entity.CouponPolicy;
import com.example.book2onandoncouponservice.entity.CouponPolicyDiscountType;
import com.example.book2onandoncouponservice.entity.CouponPolicyStatus;
import com.example.book2onandoncouponservice.entity.CouponPolicyType;
import com.example.book2onandoncouponservice.entity.MemberCoupon;
import com.example.book2onandoncouponservice.exception.CouponErrorCode;
import com.example.book2onandoncouponservice.exception.CouponIssueException;
import com.example.book2onandoncouponservice.exception.CouponNotFoundException;
import com.example.book2onandoncouponservice.exception.CouponPolicyNotFoundException;
import com.example.book2onandoncouponservice.repository.CouponPolicyRepository;
import com.example.book2onandoncouponservice.repository.CouponRepository;
import com.example.book2onandoncouponservice.repository.MemberCouponRepository;
import com.example.book2onandoncouponservice.service.impl.CouponServiceImpl;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @InjectMocks
    private CouponServiceImpl couponService;

    @Mock
    private CouponPolicyRepository policyRepository;
    @Mock
    private CouponRepository couponRepository;
    @Mock
    private MemberCouponRepository memberCouponRepository;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    // ==========================================
    // 1. createCouponUnit (쿠폰 생성)
    // ==========================================

    @Test
    @DisplayName("쿠폰 생성 성공 & Redis 동기화 로직 검증")
    void createCouponUnit_Success() {
        // given
        Long policyId = 1L;
        CouponCreateRequestDto req = new CouponCreateRequestDto(1000, policyId);

        CouponPolicy policy = mock(CouponPolicy.class);
        given(policy.getCouponPolicyStatus()).willReturn(CouponPolicyStatus.ACTIVE);
        given(policyRepository.findById(policyId)).willReturn(Optional.of(policy));

        Coupon savedCoupon = mock(Coupon.class);
        given(savedCoupon.getCouponId()).willReturn(10L);
        given(savedCoupon.getCouponRemainingQuantity()).willReturn(1000);
        given(couponRepository.save(any(Coupon.class))).willReturn(savedCoupon);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // TransactionSynchronizationManager 모킹 및 내부 로직 캡처
        try (MockedStatic<TransactionSynchronizationManager> synchronizationManager = mockStatic(
                TransactionSynchronizationManager.class)) {

            // when
            Long resultId = couponService.createCouponUnit(req);

            // then
            assertThat(resultId).isEqualTo(10L);
            verify(couponRepository).save(any(Coupon.class));

            // afterCommit 내부 로직 실행 검증
            ArgumentCaptor<TransactionSynchronization> captor = ArgumentCaptor.forClass(
                    TransactionSynchronization.class);
            synchronizationManager.verify(
                    () -> TransactionSynchronizationManager.registerSynchronization(captor.capture()));

            // 캡처한 Synchronization의 afterCommit 실행 -> Redis 로직 수행됨
            captor.getValue().afterCommit();
            verify(valueOperations).set("coupon:10stock:", "1000");
        }
    }

    @Test
    @DisplayName("쿠폰 생성 성공 - 무제한 수량(null)일 때 Redis 처리")
    void createCouponUnit_Success_Unlimited() {
        // given
        Long policyId = 1L;
        // 수량 null (무제한)
        CouponCreateRequestDto req = new CouponCreateRequestDto(null, policyId);

        CouponPolicy policy = mock(CouponPolicy.class);
        given(policy.getCouponPolicyStatus()).willReturn(CouponPolicyStatus.ACTIVE);
        given(policyRepository.findById(policyId)).willReturn(Optional.of(policy));

        Coupon savedCoupon = mock(Coupon.class);
        given(savedCoupon.getCouponId()).willReturn(10L);
        given(savedCoupon.getCouponRemainingQuantity()).willReturn(null); // 무제한
        given(couponRepository.save(any(Coupon.class))).willReturn(savedCoupon);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        try (MockedStatic<TransactionSynchronizationManager> synchronizationManager = mockStatic(
                TransactionSynchronizationManager.class)) {

            couponService.createCouponUnit(req);

            ArgumentCaptor<TransactionSynchronization> captor = ArgumentCaptor.forClass(
                    TransactionSynchronization.class);
            synchronizationManager.verify(
                    () -> TransactionSynchronizationManager.registerSynchronization(captor.capture()));

            captor.getValue().afterCommit();
            // Long.MAX_VALUE로 저장되는지 확인
            verify(valueOperations).set("coupon:10stock:", String.valueOf(Long.MAX_VALUE));
        }
    }

    @Test
    @DisplayName("쿠폰 생성 실패 - 정책 없음 (404)")
    void createCouponUnit_Fail_PolicyNotFound() {
        Long policyId = 1L;
        CouponCreateRequestDto req = new CouponCreateRequestDto(1000, policyId);

        given(policyRepository.findById(policyId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.createCouponUnit(req))
                .isInstanceOf(CouponPolicyNotFoundException.class);
    }

    @Test
    @DisplayName("쿠폰 생성 실패 - 비활성 정책 (400)")
    void createCouponUnit_Fail_DeactivePolicy() {
        Long policyId = 1L;
        CouponCreateRequestDto req = new CouponCreateRequestDto(1000, policyId);

        CouponPolicy policy = mock(CouponPolicy.class);
        given(policy.getCouponPolicyStatus()).willReturn(CouponPolicyStatus.DEACTIVE);
        given(policyRepository.findById(policyId)).willReturn(Optional.of(policy));

        assertThatThrownBy(() -> couponService.createCouponUnit(req))
                .isInstanceOf(CouponIssueException.class)
                .hasMessage(CouponErrorCode.POLICY_NOT_ISSUABLE.getMessage());
    }

    // ==========================================
    // 2. getCoupons (목록 조회)
    // ==========================================

    @Test
    @DisplayName("쿠폰 목록 조회 성공")
    void getCoupons_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        String status = "ACTIVE";

        Coupon coupon = mock(Coupon.class);
        CouponPolicy policy = mock(CouponPolicy.class);
        given(coupon.getCouponPolicy()).willReturn(policy);

        given(couponRepository.findAllByPolicyStatus(eq(CouponPolicyStatus.ACTIVE), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(coupon)));

        Page<CouponResponseDto> result = couponService.getCoupons(pageable, status);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("쿠폰 목록 조회 - 잘못된 상태값 입력 시 경고 로그 후 null 검색")
    void getCoupons_Success_InvalidStatus_CatchException() {
        Pageable pageable = PageRequest.of(0, 10);
        String status = "INVALID_STATUS_STRING"; // 존재하지 않는 Enum

        given(couponRepository.findAllByPolicyStatus(isNull(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        couponService.getCoupons(pageable, status);

        // catch 블록을 타고 policyStatus가 null인 상태로 레포지토리 호출
        verify(couponRepository).findAllByPolicyStatus(null, pageable);
    }

    @Test
    @DisplayName("쿠폰 목록 조회 - 상태값이 ALL 이면 null 검색")
    void getCoupons_Success_AllStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        String status = "ALL";

        given(couponRepository.findAllByPolicyStatus(isNull(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        couponService.getCoupons(pageable, status);

        verify(couponRepository).findAllByPolicyStatus(null, pageable);
    }


    // getCouponDetail (상세 조회)
    @Test
    @DisplayName("쿠폰 상세 조회 성공")
    void getCouponDetail_Success() {
        Long couponId = 1L;
        Coupon coupon = mock(Coupon.class);
        CouponPolicy policy = mock(CouponPolicy.class);

        given(coupon.getCouponId()).willReturn(couponId);
        given(coupon.getCouponPolicy()).willReturn(policy);
        given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));

        CouponResponseDto result = couponService.getCouponDetail(couponId);

        assertThat(result.getCouponId()).isEqualTo(couponId);
    }

    @Test
    @DisplayName("쿠폰 상세 조회 실패 - 존재하지 않음")
    void getCouponDetail_Fail_NotFound() {
        Long couponId = 1L;
        given(couponRepository.findById(couponId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.getCouponDetail(couponId))
                .isInstanceOf(CouponNotFoundException.class);
    }

    // getAvailableCoupon (사용자용 조회)

    @Test
    @DisplayName("발급 가능 쿠폰 목록 조회 성공")
    void getAvailableCoupon_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Coupon coupon = mock(Coupon.class);
        CouponPolicy policy = mock(CouponPolicy.class);
        given(coupon.getCouponPolicy()).willReturn(policy);

        given(couponRepository.findAvailableCoupons(eq(CouponPolicyStatus.ACTIVE), any(LocalDate.class), eq(pageable)))
                .willReturn(new PageImpl<>(List.of(coupon)));

        Page<CouponResponseDto> result = couponService.getAvailableCoupon(pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    // issueMemberCoupon (발급 로직) - 만료일 계산 분기 포함
    private void setupIssueMock(Long userId, Long couponId, Coupon coupon, CouponPolicy policy) {
        given(couponRepository.findByIdForUpdate(couponId)).willReturn(Optional.of(coupon));
        given(coupon.getCouponPolicy()).willReturn(policy);
        given(policy.isIssuable()).willReturn(true);
        given(memberCouponRepository.existsByUserIdAndCoupon_CouponId(userId, couponId)).willReturn(false);
    }

    @Test
    @DisplayName("발급 성공 - FixedEndDate 만료일 계산")
    void issueMemberCoupon_Success_FixedDate() {
        Long userId = 1L, couponId = 1L;
        Coupon coupon = mock(Coupon.class);
        CouponPolicy policy = mock(CouponPolicy.class);

        setupIssueMock(userId, couponId, coupon, policy);

        // Fixed Date 설정
        LocalDate endDate = LocalDate.of(2025, 12, 31);
        given(policy.getFixedEndDate()).willReturn(endDate);

        MemberCoupon savedMC = mock(MemberCoupon.class);
        given(savedMC.getMemberCouponId()).willReturn(100L);
        given(memberCouponRepository.save(any(MemberCoupon.class))).willReturn(savedMC);

        Long result = couponService.issueMemberCoupon(userId, couponId);

        assertThat(result).isEqualTo(100L);
        verify(coupon).decreaseStock();
        // save 호출 시 만료일이 2025-12-31 23:59:59... 인지 확인 (ArgumentCaptor로 정밀 검증 가능하나 여기선 호출여부만)
        verify(memberCouponRepository).save(any(MemberCoupon.class));
    }

    @Test
    @DisplayName("발급 성공 - DurationDays 만료일 계산")
    void issueMemberCoupon_Success_DurationDays() {
        Long userId = 1L, couponId = 1L;
        Coupon coupon = mock(Coupon.class);
        CouponPolicy policy = mock(CouponPolicy.class);

        setupIssueMock(userId, couponId, coupon, policy);

        // FixedDate는 null, DurationDays 설정
        given(policy.getFixedEndDate()).willReturn(null);
        given(policy.getDurationDays()).willReturn(30);

        MemberCoupon savedMC = mock(MemberCoupon.class);
        given(savedMC.getMemberCouponId()).willReturn(100L);
        given(memberCouponRepository.save(any(MemberCoupon.class))).willReturn(savedMC);

        Long result = couponService.issueMemberCoupon(userId, couponId);

        assertThat(result).isEqualTo(100L);
    }

    @Test
    @DisplayName("발급 실패 - 만료일 기준 없음 (IllegalStateException)")
    void issueMemberCoupon_Fail_NoExpirationCondition() {
        Long userId = 1L, couponId = 1L;
        Coupon coupon = mock(Coupon.class);
        CouponPolicy policy = mock(CouponPolicy.class);

        setupIssueMock(userId, couponId, coupon, policy);

        // 둘 다 null
        given(policy.getFixedEndDate()).willReturn(null);
        given(policy.getDurationDays()).willReturn(null);
        given(policy.getCouponPolicyId()).willReturn(99L);

        assertThatThrownBy(() -> couponService.issueMemberCoupon(userId, couponId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("만료일 기준이 없습니다");
    }

    @Test
    @DisplayName("발급 실패 - 쿠폰 없음")
    void issueMemberCoupon_Fail_CouponNotFound() {
        Long couponId = 1L;
        given(couponRepository.findByIdForUpdate(couponId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.issueMemberCoupon(1L, couponId))
                .isInstanceOf(CouponNotFoundException.class);
    }

    @Test
    @DisplayName("발급 실패 - 정책 발급 불가")
    void issueMemberCoupon_Fail_PolicyNotIssuable() {
        Long couponId = 1L;
        Coupon coupon = mock(Coupon.class);
        CouponPolicy policy = mock(CouponPolicy.class);

        given(couponRepository.findByIdForUpdate(couponId)).willReturn(Optional.of(coupon));
        given(coupon.getCouponPolicy()).willReturn(policy);
        given(policy.isIssuable()).willReturn(false);

        assertThatThrownBy(() -> couponService.issueMemberCoupon(1L, couponId))
                .isInstanceOf(CouponIssueException.class)
                .hasMessage(CouponErrorCode.POLICY_NOT_ISSUABLE.getMessage());
    }

    @Test
    @DisplayName("발급 실패 - 이미 발급받음")
    void issueMemberCoupon_Fail_Duplicate() {
        Long userId = 1L;
        Long couponId = 1L;
        Coupon coupon = mock(Coupon.class);
        CouponPolicy policy = mock(CouponPolicy.class);

        given(couponRepository.findByIdForUpdate(couponId)).willReturn(Optional.of(coupon));
        given(coupon.getCouponPolicy()).willReturn(policy);
        given(policy.isIssuable()).willReturn(true);
        given(memberCouponRepository.existsByUserIdAndCoupon_CouponId(userId, couponId)).willReturn(true);

        assertThatThrownBy(() -> couponService.issueMemberCoupon(userId, couponId))
                .isInstanceOf(CouponIssueException.class)
                .hasMessage(CouponErrorCode.COUPON_ALREADY_ISSUED.getMessage());
    }

    // updateAccount (수량 수정)
    @Test
    @DisplayName("쿠폰 수량 수정 성공 & Redis 업데이트")
    void updateAccount_Success() {
        Long couponId = 1L;
        int newQuantity = 500;
        Coupon coupon = mock(Coupon.class);

        given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        try (MockedStatic<TransactionSynchronizationManager> synchronizationManager = mockStatic(
                TransactionSynchronizationManager.class)) {

            Integer result = couponService.updateAccount(couponId, newQuantity);

            assertThat(result).isEqualTo(newQuantity);
            verify(coupon).update(newQuantity);

            // afterCommit 검증
            ArgumentCaptor<TransactionSynchronization> captor = ArgumentCaptor.forClass(
                    TransactionSynchronization.class);
            synchronizationManager.verify(
                    () -> TransactionSynchronizationManager.registerSynchronization(captor.capture()));
            captor.getValue().afterCommit();

            verify(valueOperations).set(anyString(), eq("500"));
        }
    }

    @Test
    @DisplayName("쿠폰 수량 수정 성공 - null(무제한) 입력")
    void updateAccount_Success_NullQuantity() {
        Long couponId = 1L;
        Integer newQuantity = null;
        Coupon coupon = mock(Coupon.class);

        given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        try (MockedStatic<TransactionSynchronizationManager> synchronizationManager = mockStatic(
                TransactionSynchronizationManager.class)) {

            couponService.updateAccount(couponId, newQuantity);

            verify(coupon).update(null);

            // afterCommit 검증
            ArgumentCaptor<TransactionSynchronization> captor = ArgumentCaptor.forClass(
                    TransactionSynchronization.class);
            synchronizationManager.verify(
                    () -> TransactionSynchronizationManager.registerSynchronization(captor.capture()));
            captor.getValue().afterCommit();

            verify(valueOperations).set(anyString(), eq(String.valueOf(Long.MAX_VALUE)));
        }
    }

    @Test
    @DisplayName("쿠폰 수량 수정 실패 - 쿠폰 없음")
    void updateAccount_Fail_NotFound() {
        given(couponRepository.findById(1L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> couponService.updateAccount(1L, 100))
                .isInstanceOf(CouponNotFoundException.class);
    }

    // ==========================================
    // 7. issueWelcomeCoupon (웰컴 쿠폰)
    // ==========================================

    @Test
    @DisplayName("웰컴 쿠폰 발급 성공")
    void issueWelcomeCoupon_Success() {
        Long userId = 1L;
        CouponPolicy policy = mock(CouponPolicy.class);
        Coupon coupon = mock(Coupon.class);

        given(policy.getCouponPolicyId()).willReturn(1L);
        given(policyRepository.findActivePolicyByType(CouponPolicyType.WELCOME)).willReturn(Optional.of(policy));
        given(couponRepository.findByCouponPolicy_CouponPolicyId(1L)).willReturn(Optional.of(coupon));

        // issueMemberCoupon Stubbing
        given(coupon.getCouponId()).willReturn(10L);
        given(couponRepository.findByIdForUpdate(10L)).willReturn(Optional.of(coupon));
        given(coupon.getCouponPolicy()).willReturn(policy);
        given(policy.isIssuable()).willReturn(true);
        given(policy.getDurationDays()).willReturn(30);

        MemberCoupon savedMC = mock(MemberCoupon.class);
        given(memberCouponRepository.save(any(MemberCoupon.class))).willReturn(savedMC);

        couponService.issueWelcomeCoupon(userId);

        verify(memberCouponRepository).save(any(MemberCoupon.class));
    }

    @Test
    @DisplayName("웰컴 쿠폰 발급 실패 - 정책 없음")
    void issueWelcomeCoupon_Fail_NoPolicy() {
        given(policyRepository.findActivePolicyByType(CouponPolicyType.WELCOME)).willReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.issueWelcomeCoupon(1L))
                .isInstanceOf(CouponPolicyNotFoundException.class);
    }

    @Test
    @DisplayName("웰컴 쿠폰 발급 실패 - 쿠폰 없음")
    void issueWelcomeCoupon_Fail_NoCoupon() {
        CouponPolicy policy = mock(CouponPolicy.class);
        given(policy.getCouponPolicyId()).willReturn(1L);

        given(policyRepository.findActivePolicyByType(CouponPolicyType.WELCOME)).willReturn(Optional.of(policy));
        given(couponRepository.findByCouponPolicy_CouponPolicyId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.issueWelcomeCoupon(1L))
                .isInstanceOf(CouponNotFoundException.class);
    }

    @Test
    @DisplayName("웰컴 쿠폰 발급 중 예외 발생 시 Rethrow")
    void issueWelcomeCoupon_Exception_Rethrow() {
        Long userId = 1L;
        CouponPolicy policy = mock(CouponPolicy.class);
        Coupon coupon = mock(Coupon.class);

        given(policy.getCouponPolicyId()).willReturn(1L);
        given(policyRepository.findActivePolicyByType(CouponPolicyType.WELCOME)).willReturn(Optional.of(policy));
        given(couponRepository.findByCouponPolicy_CouponPolicyId(1L)).willReturn(Optional.of(coupon));
        given(coupon.getCouponId()).willReturn(10L);

        // issueMemberCoupon 내부에서 예외 발생 시킴 (예: 이미 발급됨)
        given(couponRepository.findByIdForUpdate(10L)).willThrow(
                new CouponIssueException(CouponErrorCode.COUPON_ALREADY_ISSUED));

        assertThatThrownBy(() -> couponService.issueWelcomeCoupon(userId))
                .isInstanceOf(CouponIssueException.class)
                .hasMessage(CouponErrorCode.COUPON_ALREADY_ISSUED.getMessage());
    }

    // issueBirthdayCoupon (생일 쿠폰)

    @Test
    @DisplayName("생일 쿠폰 발급 성공")
    void issueBirthdayCoupon_Success() {
        Long userId = 1L;
        CouponPolicy policy = mock(CouponPolicy.class);
        Coupon coupon = mock(Coupon.class);

        given(policy.getCouponPolicyId()).willReturn(2L);
        given(policyRepository.findActivePolicyByType(CouponPolicyType.BIRTHDAY)).willReturn(Optional.of(policy));
        given(couponRepository.findByCouponPolicy_CouponPolicyId(2L)).willReturn(Optional.of(coupon));

        given(coupon.getCouponId()).willReturn(20L);
        given(couponRepository.findByIdForUpdate(20L)).willReturn(Optional.of(coupon));
        given(coupon.getCouponPolicy()).willReturn(policy);
        given(policy.isIssuable()).willReturn(true);
        given(policy.getDurationDays()).willReturn(7);

        MemberCoupon savedMC = mock(MemberCoupon.class);
        given(memberCouponRepository.save(any(MemberCoupon.class))).willReturn(savedMC);

        couponService.issueBirthdayCoupon(userId);

        verify(memberCouponRepository).save(any(MemberCoupon.class));
    }

    @Test
    @DisplayName("생일 쿠폰 발급 실패 - 정책 없음")
    void issueBirthdayCoupon_Fail_NoPolicy() {
        given(policyRepository.findActivePolicyByType(CouponPolicyType.BIRTHDAY)).willReturn(Optional.empty());
        assertThatThrownBy(() -> couponService.issueBirthdayCoupon(1L))
                .isInstanceOf(CouponPolicyNotFoundException.class);
    }

    @Test
    @DisplayName("생일 쿠폰 발급 실패 - 쿠폰 없음")
    void issueBirthdayCoupon_Fail_NoCoupon() {
        CouponPolicy policy = mock(CouponPolicy.class);
        given(policy.getCouponPolicyId()).willReturn(1L);
        given(policyRepository.findActivePolicyByType(CouponPolicyType.BIRTHDAY)).willReturn(Optional.of(policy));
        given(couponRepository.findByCouponPolicy_CouponPolicyId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.issueBirthdayCoupon(1L))
                .isInstanceOf(CouponNotFoundException.class);
    }

    @Test
    @DisplayName("생일 쿠폰 발급 중 예외 발생 시 Rethrow")
    void issueBirthdayCoupon_Exception_Rethrow() {
        Long userId = 1L;
        CouponPolicy policy = mock(CouponPolicy.class);
        Coupon coupon = mock(Coupon.class);

        given(policy.getCouponPolicyId()).willReturn(1L);
        given(policyRepository.findActivePolicyByType(CouponPolicyType.BIRTHDAY)).willReturn(Optional.of(policy));
        given(couponRepository.findByCouponPolicy_CouponPolicyId(1L)).willReturn(Optional.of(coupon));
        given(coupon.getCouponId()).willReturn(10L);

        // 예외 유발
        given(couponRepository.findByIdForUpdate(10L)).willThrow(new RuntimeException("DB Error"));

        assertThatThrownBy(() -> couponService.issueBirthdayCoupon(userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB Error");
    }

    // ==========================================
    // 9. getAppliableCoupons (적용 가능 조회)
    // ==========================================

    @Test
    @DisplayName("적용 가능 쿠폰 조회 성공")
    void getIssuableCoupons_Success() { // 메서드명 변경
        // given
        Long userId = 1L; // 회원 ID 추가
        Long bookId = 100L;
        List<Long> categories = List.of(1L);

        // 1. Policy Mock (DTO 생성에 필요한 필드 추가 Stubbing)
        CouponPolicy policy = mock(CouponPolicy.class);
        given(policy.isIssuable()).willReturn(true);
        given(policy.getCouponPolicyName()).willReturn("테스트 쿠폰");
        given(policy.getCouponPolicyDiscountType()).willReturn(CouponPolicyDiscountType.FIXED);
        given(policy.getCouponDiscountValue()).willReturn(1000);
        given(policy.getCouponPolicyStatus()).willReturn(CouponPolicyStatus.ACTIVE);

        // 2. Coupon Mock
        Coupon coupon = mock(Coupon.class);
        given(coupon.getCouponPolicy()).willReturn(policy);
        given(coupon.getCouponRemainingQuantity()).willReturn(10); // 재고 있음
        given(coupon.getCouponId()).willReturn(1L); // [중요] 서비스 로직에서 ID를 비교하므로 설정 필요

        // 3. Repository Mock
        // (1) 적용 가능 쿠폰 목록 조회
        given(couponRepository.findAppliableCoupons(bookId, categories)).willReturn(List.of(coupon));

        // (2) [추가] 유저가 보유한 쿠폰 ID 목록 조회 (회원인 경우 호출됨)
        given(memberCouponRepository.findAllCouponIdsByUserId(userId)).willReturn(List.of());

        // when
        // 파라미터에 userId 추가
        List<CouponResponseDto> result = couponService.getIssuableCoupons(userId, bookId, categories);

        // then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("적용 가능 쿠폰 조회 - 재고 없으면 제외")
    void getIssuableCoupons_FilteredByStock() { // 메소드명 변경
        Long bookId = 100L;

        // Policy Mock
        CouponPolicy policy = mock(CouponPolicy.class);
        given(policy.isIssuable()).willReturn(true); // 정책은 발급 가능 상태

        // Coupon Mock
        Coupon coupon = mock(Coupon.class);
        given(coupon.getCouponPolicy()).willReturn(policy);
        given(coupon.getCouponRemainingQuantity()).willReturn(0); // 재고 0 설정

        // Repository Mock
        given(couponRepository.findAppliableCoupons(any(), any())).willReturn(List.of(coupon));

        // when: userId는 null (비회원)로 설정하여 호출
        List<CouponResponseDto> result = couponService.getIssuableCoupons(null, bookId, List.of());

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("적용 가능 쿠폰 조회 - 정책 불가면 제외")
    void getIssuableCoupons_FilteredByPolicy() { // 메서드명 변경
        Long bookId = 100L;

        // Policy Mock
        CouponPolicy policy = mock(CouponPolicy.class);
        given(policy.isIssuable()).willReturn(false); // 발급 불가 설정

        // Coupon Mock
        Coupon coupon = mock(Coupon.class);
        given(coupon.getCouponPolicy()).willReturn(policy);
        given(coupon.getCouponRemainingQuantity()).willReturn(100);

        // Repository Mock
        given(couponRepository.findAppliableCoupons(any(), any())).willReturn(List.of(coupon));

        // when: userId를 null로 주어 비회원 상태로 조회 (MemberCouponRepository 호출 안함)
        List<CouponResponseDto> result = couponService.getIssuableCoupons(null, bookId, List.of());

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("적용 가능 쿠폰 조회 - 재고가 NULL(무제한)이면 포함")
    void getIssuableCoupons_UnlimitedStock() { // 메서드명 변경
        Long bookId = 100L;

        // Policy Mock
        CouponPolicy policy = mock(CouponPolicy.class);
        given(policy.isIssuable()).willReturn(true);
        // DTO 생성 시 필요한 값들이 있다면 추가 Stubbing 필요 (예: policy.getName() 등)
        // given(policy.getCouponPolicyName()).willReturn("Test Coupon");
        // given(policy.getCouponDiscountValue()).willReturn(1000);
        // given(policy.getCouponPolicyDiscountType()).willReturn(CouponPolicyDiscountType.FIXED);

        // Coupon Mock
        Coupon coupon = mock(Coupon.class);
        given(coupon.getCouponPolicy()).willReturn(policy);
        given(coupon.getCouponRemainingQuantity()).willReturn(null); // 무제한 재고
        given(coupon.getCouponId()).willReturn(1L); // DTO 생성 및 ID 비교 로직을 위해 ID 설정

        // Repository Mock
        given(couponRepository.findAppliableCoupons(any(), any())).willReturn(List.of(coupon));

        // when: userId null (비회원)
        List<CouponResponseDto> result = couponService.getIssuableCoupons(null, bookId, List.of());

        // then
        assertThat(result).hasSize(1);
    }

}