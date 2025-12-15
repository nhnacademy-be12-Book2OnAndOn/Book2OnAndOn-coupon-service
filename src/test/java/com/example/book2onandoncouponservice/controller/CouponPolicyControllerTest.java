package com.example.book2onandoncouponservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
import com.example.book2onandoncouponservice.exception.CouponPolicyNotFoundException;
import com.example.book2onandoncouponservice.service.CouponPolicyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CouponPolicyController.class, properties = "dooray.url=http://localhost")
class CouponPolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private CouponPolicyService couponPolicyService;

    // --- 1. getPolicies (목록 조회) ---

    @Test
    @DisplayName("정책 조회 성공 - 모든 필터 파라미터 포함")
    void getPolicies_WithAllParams() throws Exception {
        Page<CouponPolicyResponseDto> page = new PageImpl<>(Collections.emptyList());

        // given: Mocking
        given(couponPolicyService.getCouponPolicies(
                eq(CouponPolicyType.WELCOME),
                eq(CouponPolicyDiscountType.FIXED),
                eq(CouponPolicyStatus.ACTIVE),
                any(Pageable.class))).willReturn(page);

        // when & then: Enum.name()을 사용하여 정확한 문자열 전달
        mockMvc.perform(get("/admin/coupon-policies")
                        .param("type", CouponPolicyType.WELCOME.name()) // "WELCOME"
                        .param("discountType", CouponPolicyDiscountType.FIXED.name()) // "AMOUNT"
                        .param("status", CouponPolicyStatus.ACTIVE.name()) // "ACTIVE"
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("정책 조회 성공 - 파라미터 없음 (분기 테스트)")
    void getPolicies_NoParams() throws Exception {
        Page<CouponPolicyResponseDto> page = new PageImpl<>(Collections.emptyList());
        // 모든 파라미터가 null일 때
        given(couponPolicyService.getCouponPolicies(isNull(), isNull(), isNull(), any(Pageable.class)))
                .willReturn(page);

        mockMvc.perform(get("/admin/coupon-policies"))
                .andExpect(status().isOk())
                .andDo(print());
    }

    // --- 2. getPolicy (단건 조회) ---

    @Test
    @DisplayName("정책 단건 조회 성공")
    void getPolicy_Success() throws Exception {
        given(couponPolicyService.getCouponPolicy(1L)).willReturn(null);

        mockMvc.perform(get("/admin/coupon-policies/{id}", 1L))
                .andExpect(status().isOk())
                .andDo(print());
    }

    // --- 3. createPolicy (생성) ---

    @Test
    @DisplayName("정책 생성 성공")
    void createPolicy_Success() throws Exception {
        CouponPolicyRequestDto requestDto = new CouponPolicyRequestDto();

        // [필수] Validation 통과를 위해 값을 채워줍니다.
        ReflectionTestUtils.setField(requestDto, "couponPolicyName", "Test Policy");
        ReflectionTestUtils.setField(requestDto, "couponPolicyType", CouponPolicyType.WELCOME);
        ReflectionTestUtils.setField(requestDto, "couponPolicyDiscountType", CouponPolicyDiscountType.FIXED);
        ReflectionTestUtils.setField(requestDto, "couponDiscountValue", 1000);
        ReflectionTestUtils.setField(requestDto, "minPrice", 10000);

        given(couponPolicyService.createPolicy(any(CouponPolicyRequestDto.class))).willReturn(1L);

        mockMvc.perform(post("/admin/coupon-policies")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andDo(print());
    }

    @Test
    @DisplayName("정책 생성 실패 - @Valid 검증 실패")
    void createPolicy_Fail_Validation() throws Exception {
        mockMvc.perform(post("/admin/coupon-policies")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")) // 빈 객체 전송 -> @NotNull 등 위반
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    // --- 4. updatePolicy (수정) ---

    @Test
    @DisplayName("정책 수정 성공")
    void updatePolicy_Success() throws Exception {
        // given
        Long policyId = 1L;
        CouponPolicyUpdateRequestDto requestDto = new CouponPolicyUpdateRequestDto();

        // [수정] DTO 필수값 채우기 (Validation 통과용)
        ReflectionTestUtils.setField(requestDto, "couponPolicyName", "Updated Policy");
        ReflectionTestUtils.setField(requestDto, "couponPolicyType", CouponPolicyType.WELCOME);
        ReflectionTestUtils.setField(requestDto, "couponPolicyDiscountType", CouponPolicyDiscountType.FIXED);
        ReflectionTestUtils.setField(requestDto, "couponDiscountValue", 2000);
        ReflectionTestUtils.setField(requestDto, "minPrice", 5000);

        // when & then
        mockMvc.perform(put("/admin/coupon-policies/{id}", policyId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk()) // 이제 400이 아닌 200이 나올 것입니다.
                .andDo(print());

        // Verify: 서비스가 올바르게 호출되었는지 검증
        verify(couponPolicyService).updatePolicy(eq(policyId), any(CouponPolicyUpdateRequestDto.class));
    }

    @Test
    @DisplayName("정책 수정 실패 - 없는 정책 ID")
    void updatePolicy_Fail_NotFound() throws Exception {
        CouponPolicyUpdateRequestDto requestDto = new CouponPolicyUpdateRequestDto();
        // [중요] 실패 테스트지만 DTO 유효성 검사는 통과하도록 값을 채워야 함
        ReflectionTestUtils.setField(requestDto, "couponPolicyName", "Valid Name");
        ReflectionTestUtils.setField(requestDto, "couponPolicyType", CouponPolicyType.WELCOME);
        ReflectionTestUtils.setField(requestDto, "couponPolicyDiscountType", CouponPolicyDiscountType.FIXED);
        ReflectionTestUtils.setField(requestDto, "couponDiscountValue", 1000);
        ReflectionTestUtils.setField(requestDto, "minPrice", 1000);

        doThrow(new CouponPolicyNotFoundException())
                .when(couponPolicyService).updatePolicy(anyLong(), any());

        mockMvc.perform(put("/admin/coupon-policies/{id}", 99L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))) // 유효한 DTO 전송
                .andExpect(status().isNotFound())
                .andDo(print());
    }

    // --- 5. deactivatePolicy (삭제/비활성화) ---

    @Test
    @DisplayName("정책 비활성화 성공")
    void deactivatePolicy_Success() throws Exception {
        mockMvc.perform(delete("/admin/coupon-policies/{id}", 1L)
                        .with(csrf()))
                .andExpect(status().isNoContent())
                .andDo(print());

        verify(couponPolicyService).deactivatePolicy(1L);
    }
}