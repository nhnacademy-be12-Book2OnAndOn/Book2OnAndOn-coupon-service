package com.example.book2onandoncouponservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.book2onandoncouponservice.dto.request.CouponPolicyRequestDto;
import com.example.book2onandoncouponservice.dto.request.CouponPolicyUpdateRequestDto;
import com.example.book2onandoncouponservice.dto.response.CouponPolicyResponseDto;
import com.example.book2onandoncouponservice.entity.CouponPolicyDiscountType;
import com.example.book2onandoncouponservice.entity.CouponPolicyStatus;
import com.example.book2onandoncouponservice.entity.CouponPolicyType;
import com.example.book2onandoncouponservice.service.CouponPolicyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminCouponPolicyController.class)
@ActiveProfiles("test")
class AdminCouponPolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CouponPolicyService couponPolicyService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("쿠폰 정책 목록 조회 - 성공 (모든 파라미터 포함)")
    void getPolicies_Success_AllParams() throws Exception {
        // given
        // [수정] 실제 Enum 값을 사용해야 MethodArgumentTypeMismatchException 방지
        // 주의: 프로젝트의 실제 Enum 값에 맞춰 수정이 필요할 수 있습니다 (예: WELCOME, BOOK 등)
        CouponPolicyType testType = CouponPolicyType.values()[0];
        CouponPolicyDiscountType testDiscountType = CouponPolicyDiscountType.FIXED;
        CouponPolicyStatus testStatus = CouponPolicyStatus.ACTIVE;

        given(couponPolicyService.getCouponPolicies(
                eq(testType),
                eq(testDiscountType),
                eq(testStatus),
                any(Pageable.class)
        )).willReturn(new PageImpl<>(List.of()));

        // when & then
        mockMvc.perform(get("/admin/coupon-policies")
                        .param("type", testType.name()) // Enum 이름 동적 사용
                        .param("discountType", testDiscountType.name())
                        .param("status", testStatus.name())
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("쿠폰 정책 목록 조회 - 성공 (파라미터 없음 - 분기 테스트)")
    void getPolicies_Success_NoParams() throws Exception {
        // given
        given(couponPolicyService.getCouponPolicies(
                isNull(),
                isNull(),
                isNull(),
                any(Pageable.class)
        )).willReturn(new PageImpl<>(List.of()));

        // when & then
        mockMvc.perform(get("/admin/coupon-policies")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("쿠폰 정책 단건 조회 - 성공")
    void getPolicy_Success() throws Exception {
        // given
        Long policyId = 1L;
        CouponPolicyResponseDto responseDto = new CouponPolicyResponseDto();
        given(couponPolicyService.getCouponPolicy(policyId)).willReturn(responseDto);

        // when & then
        mockMvc.perform(get("/admin/coupon-policies/{coupon-policy-id}", policyId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("쿠폰 정책 생성 - 성공")
    void createPolicy_Success() throws Exception {
        // given
        CouponPolicyRequestDto requestDto = new CouponPolicyRequestDto();
        // [수정] @NotNull 필드 전부 주입 (로그 기반 필수 필드 추가)
        ReflectionTestUtils.setField(requestDto, "couponPolicyName", "Test Policy");
        ReflectionTestUtils.setField(requestDto, "couponPolicyType", CouponPolicyType.values()[0]); // 첫 번째 Enum 값 사용
        ReflectionTestUtils.setField(requestDto, "couponPolicyDiscountType", CouponPolicyDiscountType.FIXED);
        ReflectionTestUtils.setField(requestDto, "couponDiscountValue", 1000);
        ReflectionTestUtils.setField(requestDto, "minPrice", 1000);
        ReflectionTestUtils.setField(requestDto, "maxPrice", 5000);

        given(couponPolicyService.createPolicy(any(CouponPolicyRequestDto.class))).willReturn(1L);

        // when & then
        mockMvc.perform(post("/admin/coupon-policies")
                        .content(objectMapper.writeValueAsString(requestDto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andDo(print());
    }

    @Test
    @DisplayName("쿠폰 정책 생성 - 실패 (Validation: Body 없음)")
    void createPolicy_Fail_Validation() throws Exception {
        mockMvc.perform(post("/admin/coupon-policies")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is5xxServerError())
                .andDo(print());
    }

    @Test
    @DisplayName("쿠폰 정책 수정 - 성공")
    void updatePolicy_Success() throws Exception {
        // given
        Long policyId = 1L;
        CouponPolicyUpdateRequestDto requestDto = new CouponPolicyUpdateRequestDto();
        // [수정] @NotNull 필드 전부 주입
        ReflectionTestUtils.setField(requestDto, "couponPolicyName", "Updated Name");
        ReflectionTestUtils.setField(requestDto, "couponPolicyType", CouponPolicyType.values()[0]);
        ReflectionTestUtils.setField(requestDto, "couponPolicyDiscountType", CouponPolicyDiscountType.FIXED);
        ReflectionTestUtils.setField(requestDto, "couponDiscountValue", 2000);
        ReflectionTestUtils.setField(requestDto, "minPrice", 2000);

        // when & then
        mockMvc.perform(put("/admin/coupon-policies/{coupon-policy-id}", policyId)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("쿠폰 정책 수정 - 실패 (Service Exception)")
    void updatePolicy_Fail_ServiceError() throws Exception {
        // given
        Long policyId = 1L;
        CouponPolicyUpdateRequestDto requestDto = new CouponPolicyUpdateRequestDto();
        // 유효성 검사는 통과해야 서비스 로직까지 도달하므로 값 주입 필수
        ReflectionTestUtils.setField(requestDto, "couponPolicyName", "Updated Name");
        ReflectionTestUtils.setField(requestDto, "couponPolicyType", CouponPolicyType.values()[0]);
        ReflectionTestUtils.setField(requestDto, "couponPolicyDiscountType", CouponPolicyDiscountType.FIXED);
        ReflectionTestUtils.setField(requestDto, "couponDiscountValue", 2000);
        ReflectionTestUtils.setField(requestDto, "minPrice", 2000);

        // Service가 예외를 던지도록 설정
        doThrow(new RuntimeException("Policy Not Found"))
                .when(couponPolicyService).updatePolicy(eq(policyId), any(CouponPolicyUpdateRequestDto.class));

        // when & then
        mockMvc.perform(put("/admin/coupon-policies/{coupon-policy-id}", policyId)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> {
                    // 400 이상이면 에러로 간주 (404, 500 등)
                    if (result.getResponse().getStatus() < 400) {
                        throw new AssertionError("Expected error status but got " + result.getResponse().getStatus());
                    }
                })
                .andDo(print());
    }

    @Test
    @DisplayName("쿠폰 정책 비활성화(삭제) - 성공")
    void deactivatePolicy_Success() throws Exception {
        Long policyId = 1L;

        mockMvc.perform(delete("/admin/coupon-policies/{coupon-policy-id}", policyId))
                .andExpect(status().isNoContent())
                .andDo(print());
    }
}