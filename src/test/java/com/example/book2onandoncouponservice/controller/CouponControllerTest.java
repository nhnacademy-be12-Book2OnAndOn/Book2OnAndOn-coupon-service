package com.example.book2onandoncouponservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.book2onandoncouponservice.dto.request.CouponCreateRequestDto;
import com.example.book2onandoncouponservice.dto.request.CouponUpdateRequestDto;
import com.example.book2onandoncouponservice.dto.response.CouponResponseDto;
import com.example.book2onandoncouponservice.entity.CouponPolicyDiscountType;
import com.example.book2onandoncouponservice.entity.CouponPolicyStatus;
import com.example.book2onandoncouponservice.service.CouponService;
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

@WebMvcTest(CouponController.class)
class CouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CouponService couponService;

    @Autowired
    private ObjectMapper objectMapper;

    // Helper: Response DTO 생성
    private CouponResponseDto createResponseDto(Long id) {
        return new CouponResponseDto(
                id, "Test Coupon", "1000원 할인", 1000,
                CouponPolicyDiscountType.FIXED, 10000, 5000,
                30, null, null, CouponPolicyStatus.ACTIVE, 100
        );
    }

    @Test
    @DisplayName("[Admin] 쿠폰 목록 조회 - 200 OK")
    void getCoupons_Success() throws Exception {
        // given
        CouponResponseDto dto = createResponseDto(1L);
        given(couponService.getCoupons(any(Pageable.class), any()))
                .willReturn(new PageImpl<>(List.of(dto)));

        // when & then
        mockMvc.perform(get("/admin/coupons")
                        .param("page", "0")
                        .param("size", "10")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].couponId").value(1L));

        verify(couponService).getCoupons(any(Pageable.class), eq("ACTIVE"));
    }

    @Test
    @DisplayName("[Admin] 쿠폰 생성 - 201 Created")
    void createCoupon_Success() throws Exception {
        // given
        // CouponCreateRequestDto 생성자: (Integer quantity, Long couponPolicyId)
        CouponCreateRequestDto requestDto = new CouponCreateRequestDto(100, 1L);

        given(couponService.createCouponUnit(any(CouponCreateRequestDto.class))).willReturn(10L);

        // when & then
        mockMvc.perform(post("/admin/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated());

        verify(couponService).createCouponUnit(any(CouponCreateRequestDto.class));
    }

    @Test
    @DisplayName("[Admin] 쿠폰 수량 수정 - 200 OK")
    void updateCoupon_Success() throws Exception {
        // given
        Long couponId = 1L;
        CouponUpdateRequestDto requestDto = new CouponUpdateRequestDto(500);

        given(couponService.updateAccount(eq(couponId), eq(500))).willReturn(500);

        // when & then
        mockMvc.perform(put("/admin/coupons/{couponId}", couponId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(content().string("500"));

        verify(couponService).updateAccount(eq(couponId), eq(500));
    }

    @Test
    @DisplayName("[User] 발급 가능 쿠폰 조회 - 200 OK")
    void availableCoupon_Success() throws Exception {
        // given
        given(couponService.getAvailableCoupon(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(createResponseDto(1L))));

        // when & then
        mockMvc.perform(get("/coupons")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        verify(couponService).getAvailableCoupon(any(Pageable.class));
    }

    @Test
    @DisplayName("[User] 쿠폰 상세 조회 - 200 OK")
    void getCoupon_Success() throws Exception {
        // given
        Long couponId = 1L;
        given(couponService.getCouponDetail(couponId)).willReturn(createResponseDto(couponId));

        // when & then
        mockMvc.perform(get("/coupons/{couponId}", couponId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.couponId").value(couponId));

        verify(couponService).getCouponDetail(couponId);
    }

    @Test
    @DisplayName("[User] 적용 가능 쿠폰 조회 - 200 OK")
    void getAppliableCoupons_Success() throws Exception {
        // given
        Long bookId = 100L;
        List<Long> categoryIds = List.of(1L, 2L);

        given(couponService.getAppliableCoupons(eq(bookId), anyList()))
                .willReturn(List.of(createResponseDto(1L)));

        // when & then
        mockMvc.perform(get("/appliable")
                        .param("bookId", String.valueOf(bookId))
                        .param("categoryIds", "1", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].couponId").value(1L));

        verify(couponService).getAppliableCoupons(eq(bookId), anyList());
    }

    @Test
    @DisplayName("[User] 쿠폰 발급 - 201 Created")
    void issueCoupon_Success() throws Exception {
        // given
        Long userId = 123L;
        Long couponId = 1L;

        given(couponService.issueMemberCoupon(userId, couponId)).willReturn(100L); // memberCouponId

        // when & then
        mockMvc.perform(post("/coupons/{couponId}", couponId)
                        .header("X-USER-ID", userId)) // 헤더 필수
                .andExpect(status().isCreated());

        verify(couponService).issueMemberCoupon(userId, couponId);
    }
}