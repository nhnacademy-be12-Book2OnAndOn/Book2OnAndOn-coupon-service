package com.example.book2onandoncouponservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.book2onandoncouponservice.dto.request.CouponCreateRequestDto;
import com.example.book2onandoncouponservice.dto.request.CouponUpdateRequestDto;
import com.example.book2onandoncouponservice.dto.response.CouponResponseDto;
import com.example.book2onandoncouponservice.exception.CouponErrorCode;
import com.example.book2onandoncouponservice.exception.CouponIssueException;
import com.example.book2onandoncouponservice.exception.CouponNotFoundException;
import com.example.book2onandoncouponservice.service.CouponService;
import com.example.book2onandoncouponservice.service.impl.CouponIssueService;
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
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CouponController.class, properties = "dooray.url=http://localhost")
class CouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CouponService couponService;

    @MockitoBean
    private CouponIssueService couponIssueService;

    // --- 1. getCoupons (관리자 조회) ---

    @Test
    @DisplayName("관리자 쿠폰 조회 성공 - status 파라미터 있음")
    void getCoupons_WithStatus() throws Exception {
        Page<CouponResponseDto> page = new PageImpl<>(Collections.emptyList());
        given(couponService.getCoupons(any(Pageable.class), eq("ACTIVE"))).willReturn(page);

        mockMvc.perform(get("/admin/coupons")
                        .param("status", "ACTIVE")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("관리자 쿠폰 조회 성공 - status 파라미터 없음 (분기 테스트)")
    void getCoupons_WithoutStatus() throws Exception {
        Page<CouponResponseDto> page = new PageImpl<>(Collections.emptyList());
        // status가 null로 넘어가는지 확인
        given(couponService.getCoupons(any(Pageable.class), isNull())).willReturn(page);

        mockMvc.perform(get("/admin/coupons")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andDo(print());
    }

    // --- 2. createCoupon (쿠폰 생성) ---

    @Test
    @DisplayName("쿠폰 생성 성공")
    void createCoupon_Success() throws Exception {
        // given: 유효한 요청 데이터 (Reflection이나 생성자로 값 주입 필요 시 수정)
        CouponCreateRequestDto requestDto = new CouponCreateRequestDto(100, 1L); // 예시 생성자
        given(couponService.createCouponUnit(any(CouponCreateRequestDto.class))).willReturn(1L);

        mockMvc.perform(post("/admin/coupons")
                        .with(csrf()) // Security 적용 시 필요
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andDo(print());
    }

    @Test
    @DisplayName("쿠폰 생성 실패 - Validation Error (Body 누락)")
    void createCoupon_Fail_Validation() throws Exception {
        // given: 빈 객체 혹은 유효하지 않은 값 전달
        // @Valid가 붙어있으므로 필수값 누락 시 400 Bad Request
        mockMvc.perform(post("/admin/coupons")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")) // Empty Body
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    // --- 3. updateCoupon (쿠폰 수량 수정) ---

    @Test
    @DisplayName("쿠폰 수량 수정 성공")
    void updateCoupon_Success() throws Exception {
        CouponUpdateRequestDto requestDto = new CouponUpdateRequestDto();
        // requestDto.setQuantity(500); // Setter 가정

        given(couponService.updateAccount(eq(1L), any())).willReturn(500);

        mockMvc.perform(put("/admin/coupons/{couponId}", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("쿠폰 수량 수정 실패 - 존재하지 않는 쿠폰 (404)")
    void updateCoupon_Fail_NotFound() throws Exception {
        CouponUpdateRequestDto requestDto = new CouponUpdateRequestDto();

        // Service가 예외를 던질 때 Controller가 처리하는지 확인
        doThrow(new CouponNotFoundException()).when(couponService).updateAccount(anyLong(), any());

        mockMvc.perform(put("/admin/coupons/{couponId}", 999L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound()) // ExceptionHandler가 404를 반환한다고 가정
                .andDo(print());
    }

    // --- 4. availableCoupon (사용자 조회) ---

    @Test
    @DisplayName("발급 가능 쿠폰 조회 성공")
    void availableCoupon_Success() throws Exception {
        Page<CouponResponseDto> page = new PageImpl<>(Collections.emptyList());
        given(couponService.getAvailableCoupon(any(Pageable.class))).willReturn(page);

        mockMvc.perform(get("/coupons")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andDo(print());
    }

    // --- 5. getCoupon (상세 조회) ---

    @Test
    @DisplayName("쿠폰 상세 조회 성공")
    void getCoupon_Success() throws Exception {
        given(couponService.getCouponDetail(1L)).willReturn(null); // Return Null or DTO

        mockMvc.perform(get("/coupons/{couponId}", 1L))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("쿠폰 상세 조회 실패 - 404")
    void getCoupon_Fail_NotFound() throws Exception {
        given(couponService.getCouponDetail(anyLong())).willThrow(new CouponNotFoundException());

        mockMvc.perform(get("/coupons/{couponId}", 1L))
                .andExpect(status().isNotFound())
                .andDo(print());
    }

    // --- 6. getAppliableCoupons (적용 가능 쿠폰) ---

    @Test
    @DisplayName("적용 가능 쿠폰 조회 성공")
    void getAppliableCoupons_Success() throws Exception {
        given(couponService.getAppliableCoupons(anyLong(), anyList())).willReturn(Collections.emptyList());

        mockMvc.perform(get("/coupons/appliable")
                        .param("bookId", "1")
                        .param("categoryIds", "10,20"))
                .andExpect(status().isOk())
                .andDo(print());
    }

    // --- 7. issueCoupon (쿠폰 발급) ---

    @Test
    @DisplayName("쿠폰 발급 요청 성공")
    void issueCoupon_Success() throws Exception {
        // void 메서드는 doNothing (기본값이므로 생략 가능하나 명시)

        mockMvc.perform(post("/coupons/{couponId}", 1L)
                        .with(csrf())
                        .header("X-USER-ID", 123L))
                .andExpect(status().isAccepted())
                .andExpect(content().string("쿠폰 발급 요청이 접수되었습니다. 잠시 후 보관함을 확인해주세요."))
                .andDo(print());
    }

    @Test
    @DisplayName("쿠폰 발급 요청 실패 - 비즈니스 예외 (예: 이미 발급된 쿠폰)")
    void issueCoupon_Fail_Exception() throws Exception {
        // [수정] null 대신 실제 ErrorCode 사용 (예: COUPON_ALREADY_ISSUED)
        doThrow(new CouponIssueException(CouponErrorCode.COUPON_ALREADY_ISSUED))
                .when(couponIssueService).issueRequest(anyLong(), anyLong());

        mockMvc.perform(post("/coupons/{couponId}", 1L)
                        .with(csrf())
                        .header("X-USER-ID", 123L))
                // GlobalExceptionHandler가 이 예외를 400이나 409로 처리한다고 가정
                .andExpect(status().is4xxClientError())
                .andDo(print());
    }
}