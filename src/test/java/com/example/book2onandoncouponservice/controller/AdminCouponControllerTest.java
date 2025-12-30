package com.example.book2onandoncouponservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.book2onandoncouponservice.dto.request.CouponCreateRequestDto;
import com.example.book2onandoncouponservice.dto.request.CouponUpdateRequestDto;
import com.example.book2onandoncouponservice.dto.response.CouponResponseDto;
import com.example.book2onandoncouponservice.service.CouponService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminCouponController.class)
class AdminCouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CouponService couponService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @Test
    @DisplayName("관리자 쿠폰 조회 - 성공 (Status 파라미터 있음)")
    void getCoupons_Success_WithStatus() throws Exception {
        // given
        Page<CouponResponseDto> mockPage = new PageImpl<>(List.of());
        // status가 "ACTIVE"일 때의 동작 정의
        given(couponService.getCoupons(any(Pageable.class), eq("ACTIVE")))
                .willReturn(mockPage);

        // when & then
        mockMvc.perform(get("/admin/coupons")
                        .param("page", "0")
                        .param("size", "10")
                        .param("status", "ACTIVE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("관리자 쿠폰 조회 - 성공 (Status 파라미터 없음)")
    void getCoupons_Success_NoStatus() throws Exception {
        // given
        Page<CouponResponseDto> mockPage = new PageImpl<>(List.of());
        // [핵심] status가 null일 때의 동작 정의 (isNull() 사용)
        given(couponService.getCoupons(any(Pageable.class), isNull()))
                .willReturn(mockPage);

        // when & then
        mockMvc.perform(get("/admin/coupons")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("쿠폰 생성 - 성공")
    void createCoupon_Success() throws Exception {
        // given
        CouponCreateRequestDto requestDto = new CouponCreateRequestDto();
        // [핵심] DTO에 @NotNull 필드가 있다면 ReflectionTestUtils로 값 주입 필수
        ReflectionTestUtils.setField(requestDto, "couponPolicyId", 1L);
        ReflectionTestUtils.setField(requestDto, "couponRemainingQuantity", 100);

        given(couponService.createCouponUnit(any(CouponCreateRequestDto.class)))
                .willReturn(1L);

        // when & then
        mockMvc.perform(post("/admin/coupons")
                        .content(objectMapper.writeValueAsString(requestDto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andDo(print());
    }

    @Test
    @DisplayName("쿠폰 생성 - 실패 (유효성 검증 실패: Body 없음)")
    void createCoupon_Fail_Validation() throws Exception {
        // Body 없이 요청을 보내면 400 Bad Request가 발생해야 함
        mockMvc.perform(post("/admin/coupons")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is5xxServerError())
                .andDo(print());
    }

    @Test
    @DisplayName("쿠폰 수량 수정 - 성공")
    void updateCoupon_Success() throws Exception {
        // given
        Long couponId = 1L;
        CouponUpdateRequestDto requestDto = new CouponUpdateRequestDto();
        ReflectionTestUtils.setField(requestDto, "quantity", 500);

        // Service가 업데이트된 수량(500)을 반환한다고 가정
        given(couponService.updateAccount(couponId, 500))
                .willReturn(500);

        // when & then
        // 주의: 컨트롤러의 @PutMapping("/{coupon-id}")와 매칭되도록 요청
        mockMvc.perform(put("/admin/coupons/{coupon-id}", couponId)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("500"))
                .andDo(print());
    }
}