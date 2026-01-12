package com.example.book2onandoncouponservice.controller;

import com.example.book2onandoncouponservice.dto.request.CouponPolicyRequestDto;
import com.example.book2onandoncouponservice.dto.request.CouponPolicyUpdateRequestDto;
import com.example.book2onandoncouponservice.dto.response.CouponPolicyResponseDto;
import com.example.book2onandoncouponservice.entity.CouponPolicyDiscountType;
import com.example.book2onandoncouponservice.entity.CouponPolicyStatus;
import com.example.book2onandoncouponservice.entity.CouponPolicyType;
import com.example.book2onandoncouponservice.service.CouponPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/admin/coupon-policies") // 공통 경로 설정
public class AdminCouponPolicyController {

    private final CouponPolicyService couponPolicyService;

    @GetMapping
    public ResponseEntity<Page<CouponPolicyResponseDto>> getPolicies(
            @RequestParam(required = false) CouponPolicyType type,
            @RequestParam(required = false) CouponPolicyDiscountType discountType,
            @RequestParam(required = false) CouponPolicyStatus status,
            Pageable pageable) {

        Page<CouponPolicyResponseDto> policies = couponPolicyService.getCouponPolicies(type, discountType, status,
                pageable);
        return ResponseEntity.ok(policies);
    }

    @GetMapping("/{coupon-policy-id}")
    public ResponseEntity<CouponPolicyResponseDto> getPolicy(
            @PathVariable("coupon-policy-id") Long couponPolicyId) {

        CouponPolicyResponseDto responseDto = couponPolicyService.getCouponPolicy(couponPolicyId);
        return ResponseEntity.ok(responseDto);
    }

    @PostMapping
    public ResponseEntity<Void> createPolicy(
            @Valid @RequestBody CouponPolicyRequestDto requestDto) {

        Long policyId = couponPolicyService.createPolicy(requestDto);
        log.debug("쿠폰정책 생성 완료: {}", policyId);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{coupon-policy-id}")
    public ResponseEntity<Void> updatePolicy(
            @PathVariable("coupon-policy-id") Long couponPolicyId,
            @Valid @RequestBody CouponPolicyUpdateRequestDto requestDto) {

        couponPolicyService.updatePolicy(couponPolicyId, requestDto);
        log.debug("쿠폰정책 수정완료: {}", couponPolicyId);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{coupon-policy-id}")
    public ResponseEntity<Void> deactivatePolicy(
            @PathVariable("coupon-policy-id") Long couponPolicyId) {

        couponPolicyService.deactivatePolicy(couponPolicyId);
        log.debug("쿠폰정책 비활성화 완료: {}", couponPolicyId);

        return ResponseEntity.noContent().build();
    }
}