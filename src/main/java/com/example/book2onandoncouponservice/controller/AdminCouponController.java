package com.example.book2onandoncouponservice.controller;

import com.example.book2onandoncouponservice.dto.request.CouponCreateRequestDto;
import com.example.book2onandoncouponservice.dto.request.CouponUpdateRequestDto;
import com.example.book2onandoncouponservice.dto.response.CouponResponseDto;
import com.example.book2onandoncouponservice.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/coupons") // 공통 경로 설정
public class AdminCouponController {

    private final CouponService couponService;

    // 관리자 쿠폰 조회
    @GetMapping
    public ResponseEntity<Page<CouponResponseDto>> getCoupons(
            Pageable pageable,
            @RequestParam(required = false) String status) {

        Page<CouponResponseDto> coupons = couponService.getCoupons(pageable, status);
        return ResponseEntity.ok(coupons);
    }

    // 쿠폰 생성
    @PostMapping
    public ResponseEntity<Void> createCoupon(
            @Valid @RequestBody CouponCreateRequestDto requestDto) {

        Long couponId = couponService.createCouponUnit(requestDto);
        log.info("쿠폰 생성 완료: {} ", couponId);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 쿠폰 수량 수정
    @PutMapping("/{coupon-id}")
    public ResponseEntity<Integer> updateCoupon(
            @PathVariable("coupon-id") Long couponId,
            @RequestBody CouponUpdateRequestDto request) {

        return ResponseEntity.ok(couponService.updateAccount(couponId, request.getQuantity()));
    }
}