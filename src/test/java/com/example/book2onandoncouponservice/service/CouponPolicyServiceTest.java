package com.example.book2onandoncouponservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.book2onandoncouponservice.dto.request.CouponPolicyRequestDto;
import com.example.book2onandoncouponservice.dto.response.CouponPolicyResponseDto;
import com.example.book2onandoncouponservice.entity.CouponPolicy;
import com.example.book2onandoncouponservice.entity.CouponPolicyDiscountType;
import com.example.book2onandoncouponservice.entity.CouponPolicyStatus;
import com.example.book2onandoncouponservice.entity.CouponPolicyType;
import com.example.book2onandoncouponservice.exception.CouponPolicyNotFoundException;
import com.example.book2onandoncouponservice.repository.CouponPolicyRepository;
import com.example.book2onandoncouponservice.repository.CouponPolicyTargetBookRepository;
import com.example.book2onandoncouponservice.repository.CouponPolicyTargetCategoryRepository;
import com.example.book2onandoncouponservice.service.impl.CouponPolicyServiceImpl;
import java.util.Collections;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CouponPolicyServiceTest {

    @InjectMocks
    private CouponPolicyServiceImpl couponPolicyService;

    @Mock
    private CouponPolicyRepository couponPolicyRepository;

    @Mock
    private CouponPolicyTargetBookRepository targetBookRepository;

    @Mock
    private CouponPolicyTargetCategoryRepository targetCategoryRepository;

    private CouponPolicy createDummyPolicy(Long id) {
        // 빌더나 생성자 패턴에 맞게 수정 필요 (여기서는 기본 생성자 후 Reflection 사용 가정)
        CouponPolicy policy = new CouponPolicy();
        ReflectionTestUtils.setField(policy, "couponPolicyId", id);
        ReflectionTestUtils.setField(policy, "couponPolicyName", "Test Policy");
        ReflectionTestUtils.setField(policy, "couponPolicyStatus", CouponPolicyStatus.ACTIVE);
        return policy;
    }


    private CouponPolicyRequestDto createRequestDto(String name, List<Long> bookIds, List<Long> categoryIds) {
        return new CouponPolicyRequestDto(
                name,
                CouponPolicyType.CUSTOM,
                CouponPolicyDiscountType.FIXED,
                1000, // discountValue
                10000, // minPrice
                5000, // maxPrice
                30, // durationDays
                null, // fixedStartDate
                null, // fixedEndDate
                bookIds,
                categoryIds
        );
    }

    @Test
    @DisplayName("쿠폰 정책 목록 조회 성공")
    void getCouponPolicies_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<CouponPolicy> policyList = List.of(createDummyPolicy(1L), createDummyPolicy(2L));
        Page<CouponPolicy> page = new PageImpl<>(policyList);

        // 검색 필터가 모두 null일 때 findAllByFilters 호출 가정
        given(couponPolicyRepository.findAllByFilters(any(), any(), any(), any()))
                .willReturn(page);

        // when
        Page<CouponPolicyResponseDto> result = couponPolicyService.getCouponPolicies(null, null, null, pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
        verify(couponPolicyRepository).findAllByFilters(null, null, null, pageable);
    }

    @Test
    @DisplayName("쿠폰 정책 상세 조회 성공")
    void getCouponPolicy_Success() {
        // given
        Long policyId = 1L;
        CouponPolicy policy = createDummyPolicy(policyId);

        given(couponPolicyRepository.findById(policyId)).willReturn(Optional.of(policy));

        // 타겟 도서/카테고리 조회 Mocking (빈 리스트 반환)
        given(targetBookRepository.findAllByCouponPolicy_CouponPolicyId(policyId)).willReturn(Collections.emptyList());
        given(targetCategoryRepository.findAllByCouponPolicy_CouponPolicyId(policyId)).willReturn(
                Collections.emptyList());

        // when
        CouponPolicyResponseDto result = couponPolicyService.getCouponPolicy(policyId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getCouponPolicyId()).isEqualTo(policyId);
        verify(couponPolicyRepository).findById(policyId);
    }

    @Test
    @DisplayName("존재하지 않는 정책 조회 시 예외 발생")
    void getCouponPolicy_NotFound() {
        // given
        Long policyId = 999L;
        given(couponPolicyRepository.findById(policyId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> couponPolicyService.getCouponPolicy(policyId))
                .isInstanceOf(CouponPolicyNotFoundException.class);
    }

    @Test
    @DisplayName("쿠폰 정책 생성 성공")
    void createPolicy_Success() {
        // given
        // DTO 생성 (11개 인자)
        CouponPolicyRequestDto requestDto = createRequestDto(
                "New Policy",
                List.of(101L, 102L), // bookIds
                List.of(10L)         // categoryIds
        );

        CouponPolicy savedPolicy = createDummyPolicy(1L);
        given(couponPolicyRepository.save(any(CouponPolicy.class))).willReturn(savedPolicy);

        // when
        Long resultId = couponPolicyService.createPolicy(requestDto);

        // then
        assertThat(resultId).isEqualTo(1L);

        // 1. 정책 저장 호출 확인
        verify(couponPolicyRepository).save(any(CouponPolicy.class));

        // 2. 타겟 도서 저장 호출 확인 (saveAll)
        verify(targetBookRepository).saveAll(anyList());

        // 3. 타겟 카테고리 저장 호출 확인 (saveAll)
        verify(targetCategoryRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("쿠폰 정책 수정 성공")
    void updatePolicy_Success() {
        // given
        Long policyId = 1L;
        CouponPolicyRequestDto requestDto = createRequestDto(
                "Updated Name",
                List.of(201L), // 새로운 책 ID
                List.of(20L)   // 새로운 카테고리 ID
        );

        // 정책 엔티티 Mocking (updatePolicy 메서드 호출 감지용)
        CouponPolicy policy = mock(CouponPolicy.class);
        given(couponPolicyRepository.findById(policyId)).willReturn(Optional.of(policy));

        // when
        couponPolicyService.updatePolicy(policyId, requestDto);

        // then
        // 1. 엔티티 업데이트 메서드 호출 확인
        verify(policy).updatePolicy(requestDto);

        // 2. 기존 타겟 삭제 확인 (Book, Category 둘 다)
        verify(targetBookRepository).deleteByCouponPolicy_CouponPolicyId(policyId);
        verify(targetCategoryRepository).deleteByCouponPolicy_CouponPolicyId(policyId);

        // 3. 새로운 타겟 저장 확인 (Book, Category 둘 다)
        verify(targetBookRepository).saveAll(anyList());
        verify(targetCategoryRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("쿠폰 정책 수정 실패 - 정책 없음")
    void updatePolicy_NotFound() {
        // given
        Long policyId = 999L;
        CouponPolicyRequestDto requestDto = createRequestDto("Fail", null, null);

        given(couponPolicyRepository.findById(policyId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> couponPolicyService.updatePolicy(policyId, requestDto))
                .isInstanceOf(RuntimeException.class) // ServiceImpl에서 RuntimeException을 던지고 있음
                .hasMessageContaining("존재하지 않는 쿠폰정책입니다.");
    }

    @Test
    @DisplayName("쿠폰 정책 비활성화 성공")
    void deactivatePolicy_Success() {
        // given
        Long policyId = 1L;
        CouponPolicy policy = mock(CouponPolicy.class);
        given(couponPolicyRepository.findById(policyId)).willReturn(Optional.of(policy));

        // when
        couponPolicyService.deactivatePolicy(policyId);

        // then
        verify(policy).deActive(); // 상태 변경 메서드 호출 확인
    }
}