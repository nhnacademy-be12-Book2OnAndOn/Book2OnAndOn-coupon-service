package com.example.book2onandoncouponservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.book2onandoncouponservice.dto.request.CouponCreateRequestDto;
import com.example.book2onandoncouponservice.dto.response.CouponResponseDto;
import com.example.book2onandoncouponservice.entity.Coupon;
import com.example.book2onandoncouponservice.entity.CouponPolicy;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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

    // ==========================================
    // 1. createCouponUnit (쿠폰 생성)
    // ==========================================

    @Test
    @DisplayName("쿠폰 생성 성공")
    void createCouponUnit_Success() {
        // given
        Long policyId = 1L;
        CouponCreateRequestDto req = new CouponCreateRequestDto(1000, policyId);

        // Mocking
        CouponPolicy policy = mock(CouponPolicy.class);
        given(policy.getCouponPolicyStatus()).willReturn(CouponPolicyStatus.ACTIVE);
        given(policyRepository.findById(policyId)).willReturn(Optional.of(policy));

        Coupon savedCoupon = mock(Coupon.class);
        given(savedCoupon.getCouponId()).willReturn(10L);
        given(couponRepository.save(any(Coupon.class))).willReturn(savedCoupon);

        // when
        Long resultId = couponService.createCouponUnit(req);

        // then
        assertThat(resultId).isEqualTo(10L);
        verify(couponRepository).save(any(Coupon.class));
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
        given(coupon.getCouponPolicy()).willReturn(policy); // DTO 생성 시 필요

        given(couponRepository.findAllByPolicyStatus(eq(CouponPolicyStatus.ACTIVE), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(coupon)));

        Page<CouponResponseDto> result = couponService.getCoupons(pageable, status);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("쿠폰 목록 조회 성공 - 잘못된 상태값은 전체 조회")
    void getCoupons_Success_InvalidStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        String status = "INVALID";

        given(couponRepository.findAllByPolicyStatus(isNull(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        couponService.getCoupons(pageable, status);

        verify(couponRepository).findAllByPolicyStatus(null, pageable);
    }

    // ==========================================
    // 3. getCouponDetail (상세 조회)
    // ==========================================

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

    // ==========================================
    // 4. getAvailableCoupon (사용자용 조회)
    // ==========================================

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

    // ==========================================
    // 5. issueMemberCoupon (발급 로직)
    // ==========================================

    @Test
    @DisplayName("회원 쿠폰 발급 성공")
    void issueMemberCoupon_Success() {
        Long userId = 1L;
        Long couponId = 1L;

        Coupon coupon = mock(Coupon.class);
        CouponPolicy policy = mock(CouponPolicy.class);

        given(couponRepository.findByIdForUpdate(couponId)).willReturn(Optional.of(coupon));
        given(coupon.getCouponPolicy()).willReturn(policy);
        given(policy.isIssuable()).willReturn(true);
        given(memberCouponRepository.existsByUserIdAndCoupon_CouponId(userId, couponId)).willReturn(false);

        MemberCoupon savedMC = mock(MemberCoupon.class);
        given(savedMC.getMemberCouponId()).willReturn(100L);
        given(memberCouponRepository.save(any(MemberCoupon.class))).willReturn(savedMC);

        Long result = couponService.issueMemberCoupon(userId, couponId);

        assertThat(result).isEqualTo(100L);
        verify(coupon).decreaseStock();
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

    @Test
    @DisplayName("발급 실패 - 재고 소진")
    void issueMemberCoupon_Fail_OutOfStock() {
        Long userId = 1L;
        Long couponId = 1L;
        Coupon coupon = mock(Coupon.class);
        CouponPolicy policy = mock(CouponPolicy.class);

        given(couponRepository.findByIdForUpdate(couponId)).willReturn(Optional.of(coupon));
        given(coupon.getCouponPolicy()).willReturn(policy);
        given(policy.isIssuable()).willReturn(true);
        given(memberCouponRepository.existsByUserIdAndCoupon_CouponId(userId, couponId)).willReturn(false);

        // Mock 객체에서 예외 던지기
        doThrow(new CouponIssueException(CouponErrorCode.COUPON_OUT_OF_STOCK))
                .when(coupon).decreaseStock();

        assertThatThrownBy(() -> couponService.issueMemberCoupon(userId, couponId))
                .isInstanceOf(CouponIssueException.class)
                .hasMessage(CouponErrorCode.COUPON_OUT_OF_STOCK.getMessage());
    }

    // ==========================================
    // 6. updateAccount (수량 수정)
    // ==========================================

    @Test
    @DisplayName("쿠폰 수량 수정 성공")
    void updateAccount_Success() {
        Long couponId = 1L;
        int newQuantity = 500;
        Coupon coupon = mock(Coupon.class);

        given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));

        Integer result = couponService.updateAccount(couponId, newQuantity);

        assertThat(result).isEqualTo(newQuantity);
        verify(coupon).update(newQuantity);
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
        given(policyRepository.findByCouponPolicyType(CouponPolicyType.WELCOME)).willReturn(Optional.of(policy));
        given(couponRepository.findByCouponPolicy_CouponPolicyId(1L)).willReturn(Optional.of(coupon));

        // issueMemberCoupon 내부 Stubbing
        given(coupon.getCouponId()).willReturn(10L);
        given(couponRepository.findByIdForUpdate(10L)).willReturn(Optional.of(coupon));
        given(coupon.getCouponPolicy()).willReturn(policy); // 여기서 정책 반환
        given(policy.isIssuable()).willReturn(true); // 정책 유효

        MemberCoupon savedMC = mock(MemberCoupon.class);
        given(memberCouponRepository.save(any(MemberCoupon.class))).willReturn(savedMC);

        couponService.issueWelcomeCoupon(userId);

        verify(memberCouponRepository).save(any(MemberCoupon.class));
    }

    @Test
    @DisplayName("웰컴 쿠폰 발급 실패 - 정책 없음")
    void issueWelcomeCoupon_Fail_NoPolicy() {
        given(policyRepository.findByCouponPolicyType(CouponPolicyType.WELCOME)).willReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.issueWelcomeCoupon(1L))
                .isInstanceOf(CouponPolicyNotFoundException.class);
    }

    @Test
    @DisplayName("웰컴 쿠폰 발급 실패 - 쿠폰 없음")
    void issueWelcomeCoupon_Fail_NoCoupon() {
        CouponPolicy policy = mock(CouponPolicy.class);
        given(policy.getCouponPolicyId()).willReturn(1L);

        given(policyRepository.findByCouponPolicyType(CouponPolicyType.WELCOME)).willReturn(Optional.of(policy));
        given(couponRepository.findByCouponPolicy_CouponPolicyId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.issueWelcomeCoupon(1L))
                .isInstanceOf(CouponNotFoundException.class);
    }

    // ==========================================
    // 8. issueBirthdayCoupon (생일 쿠폰)
    // ==========================================

    @Test
    @DisplayName("생일 쿠폰 발급 성공")
    void issueBirthdayCoupon_Success() {
        Long userId = 1L;
        CouponPolicy policy = mock(CouponPolicy.class);
        Coupon coupon = mock(Coupon.class);

        given(policy.getCouponPolicyId()).willReturn(2L);
        given(policyRepository.findByCouponPolicyType(CouponPolicyType.BIRTHDAY)).willReturn(Optional.of(policy));
        given(couponRepository.findByCouponPolicy_CouponPolicyId(2L)).willReturn(Optional.of(coupon));

        // issueMemberCoupon 내부 Stubbing
        given(coupon.getCouponId()).willReturn(20L);
        given(couponRepository.findByIdForUpdate(20L)).willReturn(Optional.of(coupon));
        given(coupon.getCouponPolicy()).willReturn(policy);
        given(policy.isIssuable()).willReturn(true);

        MemberCoupon savedMC = mock(MemberCoupon.class);
        given(memberCouponRepository.save(any(MemberCoupon.class))).willReturn(savedMC);

        couponService.issueBirthdayCoupon(userId);

        verify(memberCouponRepository).save(any(MemberCoupon.class));
    }

    // ==========================================
    // 9. getAppliableCoupons (적용 가능 조회)
    // ==========================================

    @Test
    @DisplayName("적용 가능 쿠폰 조회 성공")
    void getAppliableCoupons_Success() {
        Long bookId = 100L;
        List<Long> categories = List.of(1L);

        CouponPolicy policy = mock(CouponPolicy.class);
        given(policy.isIssuable()).willReturn(true);

        Coupon coupon = mock(Coupon.class);
        given(coupon.getCouponPolicy()).willReturn(policy);
        given(coupon.getCouponRemainingQuantity()).willReturn(10); // 재고 있음

        given(couponRepository.findAppliableCoupons(bookId, categories)).willReturn(List.of(coupon));

        List<CouponResponseDto> result = couponService.getAppliableCoupons(bookId, categories);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("적용 가능 쿠폰 조회 - 재고 없으면 제외")
    void getAppliableCoupons_FilteredByStock() {
        Long bookId = 100L;

        CouponPolicy policy = mock(CouponPolicy.class);
        Coupon coupon = mock(Coupon.class);
        given(coupon.getCouponPolicy()).willReturn(policy);
        given(coupon.getCouponRemainingQuantity()).willReturn(0); // 재고 없음

        given(couponRepository.findAppliableCoupons(any(), any())).willReturn(List.of(coupon));

        List<CouponResponseDto> result = couponService.getAppliableCoupons(bookId, List.of());

        assertThat(result).isEmpty();
    }
}