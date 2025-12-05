package com.example.book2onandoncouponservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CouponPolicyController.class)
class CouponPolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CouponPolicyService couponPolicyService;

    @Autowired
    private ObjectMapper objectMapper;

    // Helper: Request DTO 생성
    private CouponPolicyRequestDto createRequestDto() {
        return new CouponPolicyRequestDto(
                "Test Policy",
                CouponPolicyType.CUSTOM,
                CouponPolicyDiscountType.FIXED,
                1000, 0, 0, 30,
                null, null,
                List.of(1L), List.of(10L)
        );
    }

    @Test
    @DisplayName("쿠폰 정책 목록 조회 (필터링 포함) - 200 OK")
    void getPolicies_Success() throws Exception {
        // given
        CouponPolicyResponseDto responseDto = new CouponPolicyResponseDto(
                1L, "Test",
                CouponPolicyType.CUSTOM, // [수정] 올바른 Enum 사용
                CouponPolicyDiscountType.FIXED,
                1000, 0, 0, 30, null, null,
                List.of(), List.of(), CouponPolicyStatus.ACTIVE
        );

        given(couponPolicyService.getCouponPolicies(any(), any(), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(responseDto)));

        // when & then
        mockMvc.perform(get("/admin/coupon-policies")
                        .param("page", "0")
                        .param("size", "10")
                        .param("type", "CUSTOM") // [수정]
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].couponPolicyName").value("Test"));

        verify(couponPolicyService).getCouponPolicies(eq(CouponPolicyType.CUSTOM), eq(null),
                eq(CouponPolicyStatus.ACTIVE), any(Pageable.class));
    }

    @Test
    @DisplayName("쿠폰 정책 상세 조회 - 200 OK")
    void getPolicy_Success() throws Exception {
        // given
        Long policyId = 1L;
        CouponPolicyResponseDto responseDto = new CouponPolicyResponseDto(
                policyId, "Detail",
                CouponPolicyType.CUSTOM, // [수정]
                CouponPolicyDiscountType.FIXED,
                1000, 0, 0, 30, null, null,
                List.of(), List.of(), CouponPolicyStatus.ACTIVE
        );

        given(couponPolicyService.getCouponPolicy(policyId)).willReturn(responseDto);

        // when & then
        mockMvc.perform(get("/admin/coupon-policies/{id}", policyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.couponPolicyId").value(policyId));
    }

    @Test
    @DisplayName("쿠폰 정책 생성 - 201 Created")
    void createPolicy_Success() throws Exception {
        // given
        CouponPolicyRequestDto requestDto = createRequestDto();
        given(couponPolicyService.createPolicy(any(CouponPolicyRequestDto.class))).willReturn(1L);

        // when & then
        mockMvc.perform(post("/admin/coupon-policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated());

        verify(couponPolicyService).createPolicy(any(CouponPolicyRequestDto.class));
    }

    @Test
    @DisplayName("쿠폰 정책 수정 - 200 OK")
    void updatePolicy_Success() throws Exception {
        // given
        Long policyId = 1L;
        CouponPolicyRequestDto requestDto = createRequestDto();

        // when & then
        mockMvc.perform(put("/admin/coupon-policies/{id}", policyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk());

        verify(couponPolicyService).updatePolicy(eq(policyId), any(CouponPolicyUpdateRequestDto.class));
    }

    @Test
    @DisplayName("쿠폰 정책 비활성화 (삭제) - 204 No Content")
    void deactivatePolicy_Success() throws Exception {
        // given
        Long policyId = 1L;

        // when & then
        mockMvc.perform(delete("/admin/coupon-policies/{id}", policyId))
                .andExpect(status().isNoContent());

        verify(couponPolicyService).deactivatePolicy(policyId);
    }
}