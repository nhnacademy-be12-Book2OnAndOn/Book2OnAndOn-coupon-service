package com.example.book2onandoncouponservice.controller;

import com.example.book2onandoncouponservice.dto.request.CouponPolicyRequestDto;
import com.example.book2onandoncouponservice.dto.response.CouponPolicyResponseDto;
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
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/coupon-policies")
public class CouponPolicyController {
    private final CouponPolicyService couponPolicyService;

    @GetMapping
    public ResponseEntity<Page<CouponPolicyResponseDto>> getPolicies(Pageable pageable) {

        Page<CouponPolicyResponseDto> policies = couponPolicyService.getCouponPolicies(pageable);
        return ResponseEntity.ok(policies);
    }

    @GetMapping("/{couponPolicyId}")
    public ResponseEntity<CouponPolicyResponseDto> getPolicy(
            @PathVariable("couponPolicyId") Long couponPolicyId) {

        CouponPolicyResponseDto responseDto = couponPolicyService.getCouponPolicy(couponPolicyId);

        return ResponseEntity.ok(responseDto);
    }

    @PostMapping
    public ResponseEntity<Void> createPolicy(
            @Valid @RequestBody CouponPolicyRequestDto requestDto) {

        Long policyId = couponPolicyService.createPolicy(requestDto);
        log.info("쿠폰정책 생성 완료: {}", policyId);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{couponPolicyId}")
    public ResponseEntity<Void> updatePolicy(
            @PathVariable Long couponPolicyId,
            @Valid @RequestBody CouponPolicyRequestDto requestDto) {

        couponPolicyService.updatePolicy(couponPolicyId, requestDto);
        log.info("쿠폰정책 수정완료: {}", couponPolicyId);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{couponPolicyId}")
    public ResponseEntity<Void> deactivatePolicy(
            @PathVariable Long couponPolicyId) {

        couponPolicyService.deactivatePolicy(couponPolicyId);
        log.warn("쿠폰정책 비활성화 완료: {}", couponPolicyId);

        return ResponseEntity.noContent().build();
    }
}
