package com.example.book2onandoncouponservice.controller;

import com.example.book2onandoncouponservice.dto.request.CouponPolicyCreateRequestDto;
import com.example.book2onandoncouponservice.dto.request.CouponPolicyUpdateRequestDto;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/coupon-policies")
public class CouponPolicyController {
    private final CouponPolicyService couponPolicyService;
    private static final String ADMIN_ROLE = "COUPON_ADMIN";

    private void checkAuthorization(String userRole) {
        if (!ADMIN_ROLE.equals(userRole)) {
            log.warn("접근권한 없는 사용자 접근시도");
            throw new RuntimeException("접근권한이 없습니다.");
        }
    }

    @GetMapping
    public ResponseEntity<Page<CouponPolicyResponseDto>> getPolicies(
            @RequestHeader("X-USER-ROLE") String userRole,
            Pageable pageable) {

        checkAuthorization(userRole);

        Page<CouponPolicyResponseDto> policies = couponPolicyService.getCouponPolicies(pageable);
        return ResponseEntity.ok(policies);
    }

    @GetMapping("/{couponPolicyId}")
    public ResponseEntity<CouponPolicyResponseDto> getPolicy(
            @RequestHeader("X-USER-ROLE") String userRole,
            @PathVariable("couponPolicyId") Long couponPolicyId) {

        checkAuthorization(userRole);

        CouponPolicyResponseDto responseDto = couponPolicyService.getCouponPolicy(couponPolicyId);

        return ResponseEntity.ok(responseDto);
    }

    @PostMapping
    public ResponseEntity<Void> createPolicy(
            @RequestHeader("X-USER-ROLE") String userRole,
            @Valid @RequestBody CouponPolicyCreateRequestDto requestDto) {

        checkAuthorization(userRole);

        Long policyId = couponPolicyService.createPolicy(requestDto);
        log.info("쿠폰정책 생성 완료: {}", policyId);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{couponPolicyId}")
    public ResponseEntity<Void> updatePolicy(
            @RequestHeader("X-USER-ROLE") String userRole,
            @PathVariable Long couponPolicyId,
            @Valid @RequestBody CouponPolicyUpdateRequestDto requestDto) {

        checkAuthorization(userRole);

        couponPolicyService.updatePolicy(couponPolicyId, requestDto);
        log.info("쿠폰정책 수정완료: {}", couponPolicyId);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{couponPolicyId}")
    public ResponseEntity<Void> deactivatePolicy(
            @RequestHeader("X-USER-ROLE") String userRole,
            @PathVariable Long couponPolicyId) {

        checkAuthorization(userRole);

        couponPolicyService.deactivatePolicy(couponPolicyId);
        log.warn("쿠폰정책 비활성화 완료: {}", couponPolicyId);

        return ResponseEntity.noContent().build();
    }
}
