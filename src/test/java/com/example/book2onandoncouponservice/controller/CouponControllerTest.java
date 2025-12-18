package com.example.book2onandoncouponservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.book2onandoncouponservice.dto.response.CouponResponseDto;
import com.example.book2onandoncouponservice.service.CouponService;
import com.example.book2onandoncouponservice.service.impl.CouponIssueService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CouponController.class)
@ActiveProfiles("test")
class CouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CouponService couponService;

    @MockitoBean
    private CouponIssueService couponIssueService;

    @Test
    @DisplayName("사용자용 발급 가능 쿠폰 목록 조회 - 성공")
    void availableCoupon_Success() throws Exception {
        // given
        given(couponService.getAvailableCoupon(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        // when & then
        mockMvc.perform(get("/coupons")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("쿠폰 상세 조회 - 성공")
    void getCoupon_Success() throws Exception {
        // given
        Long couponId = 1L;
        CouponResponseDto responseDto = new CouponResponseDto(); // 기본 생성자 사용
        given(couponService.getCouponDetail(couponId)).willReturn(responseDto);

        // when & then
        // Controller에서 @PathVariable("coupon-id")로 명시했으므로 URL 템플릿 변수명 일치 확인
        mockMvc.perform(get("/coupons/{coupon-id}", couponId))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("도서 상세페이지 적용(발급) 가능 쿠폰 조회 - 성공")
    void getAppliableCoupons_Success() throws Exception {
        // given
        Long bookId = 100L;
        List<Long> categoryIds = List.of(1L, 2L);

        given(couponService.getAppliableCoupons(eq(bookId), anyList()))
                .willReturn(List.of());

        // when & then
        mockMvc.perform(get("/coupons/issuable")
                        .param("bookId", String.valueOf(bookId))
                        .param("categoryIds", "1", "2")) // List 파라미터 전달 방식
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("도서 상세페이지 적용 가능 쿠폰 조회 - 실패 (파라미터 누락)")
    void getAppliableCoupons_Fail_MissingParams() throws Exception {
        // given
        // 필수 파라미터 bookId, categoryIds 누락

        // when & then
        mockMvc.perform(get("/coupons/issuable"))
                .andExpect(status().is5xxServerError())
                .andDo(print());
    }

    @Test
    @DisplayName("쿠폰 발급 요청 - 성공")
    void issueCoupon_Success() throws Exception {
        // given
        Long userId = 999L;
        Long couponId = 1L;

        // when & then
        mockMvc.perform(post("/coupons/{coupon-id}", couponId)
                        .header("X-USER-ID", userId))
                .andExpect(status().isAccepted()) // 202 Accepted
                .andExpect(content().string("쿠폰 발급 요청이 접수되었습니다. 잠시 후 보관함을 확인해주세요."))
                .andDo(print());

        // Service 메서드 호출 검증
        verify(couponIssueService).issueRequest(userId, couponId);
    }

    @Test
    @DisplayName("쿠폰 발급 요청 - 실패 (헤더 누락)")
    void issueCoupon_Fail_MissingHeader() throws Exception {
        // given
        Long couponId = 1L;
        // X-USER-ID 헤더 누락

        // when & then
        mockMvc.perform(post("/coupons/{coupon-id}", couponId))
                .andExpect(status().is5xxServerError())
                .andDo(print());
    }
}