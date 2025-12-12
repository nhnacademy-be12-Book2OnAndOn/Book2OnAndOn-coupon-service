package com.example.book2onandoncouponservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.book2onandoncouponservice.dto.request.OrderCouponCheckRequestDto;
import com.example.book2onandoncouponservice.dto.request.UseCouponRequestDto;
import com.example.book2onandoncouponservice.dto.response.MemberCouponResponseDto;
import com.example.book2onandoncouponservice.entity.CouponPolicyDiscountType;
import com.example.book2onandoncouponservice.entity.MemberCouponStatus;
import com.example.book2onandoncouponservice.service.MemberCouponService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
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

@WebMvcTest(MemberCouponController.class)
class MemberCouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberCouponService memberCouponService;

    @Autowired
    private ObjectMapper objectMapper;

    // 1. 쿠폰 사용 (POST /my-coupon/{id}/use)
    @Test
    @DisplayName("쿠폰 사용 - 200 OK")
    void useCoupon_Success() throws Exception {
        // given
        Long memberCouponId = 1L;
        Long userId = 100L;
        Long orderId = 12345L;

        // Body에 담을 DTO
        UseCouponRequestDto requestDto = new UseCouponRequestDto(orderId);

        // when & then
        mockMvc.perform(post("/my-coupon/{memberCouponId}/use", memberCouponId)
                        .header("X-USER-ID", userId) // 필수 헤더
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))) // JSON Body 전송
                .andExpect(status().isOk());

        // Verify: Service에 orderId가 잘 전달되었는지 확인
        verify(memberCouponService).useMemberCoupon(memberCouponId, userId, orderId);
    }

    // 2. 내 쿠폰 목록 조회 (GET /my-coupon)
    @Test
    @DisplayName("내 쿠폰 목록 조회 - 200 OK")
    void getMyCoupons_Success() throws Exception {
        // given
        Long userId = 100L;

        // Response DTO 생성
        MemberCouponResponseDto dto = new MemberCouponResponseDto(
                1L, "My Coupon", 0, 0, 1000, CouponPolicyDiscountType.FIXED,
                MemberCouponStatus.NOT_USED, LocalDateTime.now(), null, "Desc"
        );

        given(memberCouponService.getMyCoupon(eq(userId), any(Pageable.class), eq("USED")))
                .willReturn(new PageImpl<>(List.of(dto)));

        // when & then
        mockMvc.perform(get("/my-coupon")
                        .header("X-USER-ID", userId) // 필수 헤더
                        .param("page", "0")
                        .param("size", "10")
                        .param("status", "USED")) // 파라미터
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].couponName").value("My Coupon"));

        // Verify
        verify(memberCouponService).getMyCoupon(eq(userId), any(Pageable.class), eq("USED"));
    }

    // 3. 주문 적용 가능 쿠폰 조회 (POST /my-coupon/usable)
    @Test
    @DisplayName("주문 적용 가능 쿠폰 조회 - 200 OK")
    void getUsableCoupons_Success() throws Exception {
        // given
        Long userId = 100L;

        // Request DTO 생성 (도서 ID 리스트, 카테고리 ID 리스트)
        OrderCouponCheckRequestDto requestDto = new OrderCouponCheckRequestDto(
                List.of(1001L, 1002L), // bookIds
                List.of(10L, 20L)      // categoryIds
        );

        // 예상되는 Response DTO 생성
        MemberCouponResponseDto responseDto = new MemberCouponResponseDto(
                2L, "Possible Coupon", 0, 0, 3000, CouponPolicyDiscountType.FIXED,
                MemberCouponStatus.NOT_USED, LocalDateTime.now().plusDays(7), null, "Order Discount"
        );

        // Service Mocking
        given(memberCouponService.getUsableCoupons(eq(userId), any(OrderCouponCheckRequestDto.class)))
                .willReturn(List.of(responseDto));

        // when & then
        mockMvc.perform(post("/my-coupon/usable") // Controller 클래스 레벨 매핑이 "/my-coupon"이라고 가정
                        .header("X-USER-ID", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].memberCouponId").value(2L))
                .andExpect(jsonPath("$[0].couponName").value("Possible Coupon"))
                .andExpect(jsonPath("$[0].discountValue").value(3000));

        // Verify: Service가 올바른 파라미터로 호출되었는지 검증
        verify(memberCouponService).getUsableCoupons(eq(userId), any(OrderCouponCheckRequestDto.class));
    }
}