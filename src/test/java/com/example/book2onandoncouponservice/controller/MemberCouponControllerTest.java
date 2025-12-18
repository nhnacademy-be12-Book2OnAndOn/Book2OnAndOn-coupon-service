package com.example.book2onandoncouponservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.book2onandoncouponservice.dto.request.OrderCouponCheckRequestDto;
import com.example.book2onandoncouponservice.dto.request.UseCouponRequestDto;
import com.example.book2onandoncouponservice.service.MemberCouponService;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MemberCouponController.class)
class MemberCouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberCouponService memberCouponService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("쿠폰 사용 요청 - 성공")
    void useCoupon_Success() throws Exception {
        Long memberCouponId = 1L;
        Long userId = 100L;
        UseCouponRequestDto requestDto = new UseCouponRequestDto();
        // requestDto.setOrderId(123L); // DTO 값 설정

        mockMvc.perform(post("/my-coupon/{member-coupon-id}/use", memberCouponId)
                        .header("X-USER-ID", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("내 쿠폰 목록 조회 - 성공 (Status 있음)")
    void getMyCoupons_Success_WithStatus() throws Exception {
        Long userId = 100L;
        given(memberCouponService.getMyCoupon(eq(userId), any(Pageable.class), anyString()))
                .willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/my-coupon")
                        .header("X-USER-ID", userId)
                        .param("status", "USED"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("내 쿠폰 목록 조회 - 성공 (Status 없음 - 분기)")
    void getMyCoupons_Success_NoStatus() throws Exception {
        Long userId = 100L;
        given(memberCouponService.getMyCoupon(eq(userId), any(Pageable.class), isNull()))
                .willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/my-coupon")
                        .header("X-USER-ID", userId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("주문 시 사용 가능 쿠폰 조회 - 성공")
    void getUsableCoupons_Success() throws Exception {
        // given
        Long userId = 100L;
        OrderCouponCheckRequestDto requestDto = new OrderCouponCheckRequestDto();

        // [수정] DTO의 필수 필드(@NotNull) 값 주입
        ReflectionTestUtils.setField(requestDto, "bookIds", List.of(1L, 2L));
        ReflectionTestUtils.setField(requestDto, "categoryIds", List.of(10L));

        // 서비스 메서드 호출 시 빈 리스트 반환하도록 스터빙
        given(memberCouponService.getUsableCoupons(eq(userId), any(OrderCouponCheckRequestDto.class)))
                .willReturn(List.of());

        // when & then
        mockMvc.perform(post("/my-coupon/usable")
                        .header("X-USER-ID", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk()); // 200 OK 확인
    }

    @Test
    @DisplayName("쿠폰 적용 대상 조회")
    void getCouponTargets_Success() throws Exception {
        Long memberCouponId = 1L;

        mockMvc.perform(get("/my-coupon/{member-coupon-id}/targets", memberCouponId))
                .andExpect(status().isOk());
    }
}