package com.example.book2onandoncouponservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.book2onandoncouponservice.dto.response.MemberCouponResponseDto;
import com.example.book2onandoncouponservice.entity.CouponPolicyDiscountType;
import com.example.book2onandoncouponservice.entity.MemberCouponStatus;
import com.example.book2onandoncouponservice.service.MemberCouponService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MemberCouponController.class)
class MemberCouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberCouponService memberCouponService;

    @Test
    @DisplayName("쿠폰 사용 - 200 OK")
    void useCoupon_Success() throws Exception {
        // given
        Long memberCouponId = 1L;
        Long userId = 100L;

        // when & then
        mockMvc.perform(post("/my-coupon/{memberCouponId}/use", memberCouponId)
                        .header("X-USER-ID", userId)) // 헤더 검증
                .andExpect(status().isOk());

        verify(memberCouponService).useMemberCoupon(memberCouponId, userId);
    }

    @Test
    @DisplayName("내 쿠폰 목록 조회 - 200 OK")
    void getMyCoupons_Success() throws Exception {
        // given
        Long userId = 100L;
        MemberCouponResponseDto dto = new MemberCouponResponseDto(
                1L, "My Coupon", 0, 0, 1000, CouponPolicyDiscountType.FIXED,
                MemberCouponStatus.NOT_USED, LocalDateTime.now(), null, "Desc"
        );

        given(memberCouponService.getMyCoupon(eq(userId), any(Pageable.class), eq("USED")))
                .willReturn(new PageImpl<>(List.of(dto)));

        // when & then
        mockMvc.perform(get("/my-coupon")
                        .header("X-USER-ID", userId)
                        .param("page", "0")
                        .param("size", "10")
                        .param("status", "USED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].couponName").value("My Coupon"));

        verify(memberCouponService).getMyCoupon(eq(userId), any(Pageable.class), eq("USED"));
    }

    @Test
    @DisplayName("쿠폰 사용 취소 - 200 OK")
    void cancelCoupon_Success() throws Exception {
        // given
        Long memberCouponId = 1L;
        Long userId = 100L;

        // when & then
        mockMvc.perform(post("/my-coupon/{memberCouponId}/cancel", memberCouponId)
                        .header("X-USER-ID", userId))
                .andExpect(status().isOk());

        verify(memberCouponService).cancelMemberCoupon(memberCouponId, userId);
    }
}