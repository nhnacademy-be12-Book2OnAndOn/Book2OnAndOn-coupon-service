package com.example.book2onandoncouponservice.controller;

import com.example.book2onandoncouponservice.dto.request.CouponIssueRequestDto;
import com.example.book2onandoncouponservice.dto.request.UseCouponRequestDto;
import com.example.book2onandoncouponservice.dto.response.CouponResponseDto;
import com.example.book2onandoncouponservice.service.CouponService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CouponController {
    private final CouponService couponService;

    @GetMapping("/users/me/coupons")
    public ResponseEntity<List<CouponResponseDto>> getMyCoupons(@RequestHeader("X-USER-ID") Long userId) {

        log.info("쿠폰 조회 - userId: {}", userId);
        List<CouponResponseDto> coupons = couponService.getMyCoupons(userId);

        return ResponseEntity.ok(coupons);
    }

    @PostMapping("/coupons/download/{policyId}")
    public ResponseEntity<Long> downloadCoupon(@RequestHeader("X-USER-ID") Long userId,
                                               @PathVariable("policyId") Long policyId) {

        log.info("사용자 쿠폰 발급 시도 - userId: {}, policyId: {}", userId, policyId);

        Long couponId = couponService.issueCoupon(userId, policyId);

        log.info("사용자 쿠폰 발급 완료 - couponId: {}", couponId);
        return ResponseEntity.status(HttpStatus.CREATED).body(couponId);
    }

    //필요할진 모르겠음 보류
    @PostMapping("/admin/coupons/issue")
    public ResponseEntity<Long> issueCoupon(@Valid @RequestBody CouponIssueRequestDto requestDto) {

        log.info("쿠폰 발급 시도 - userId: {}, policyId: {}", requestDto.getUserId(), requestDto.getCouponPolicyId());
        Long couponId = couponService.issueCoupon(requestDto.getUserId(), requestDto.getCouponPolicyId());

        log.info("쿠폰 발급 완료 - couponId: {}", couponId);
        return ResponseEntity.status(HttpStatus.CREATED).body(couponId);
    }

    @PutMapping("/coupons/{couponId}/use")
    public ResponseEntity<Void> useCoupon(@RequestHeader("X-USER-ID") Long userId,
                                          @PathVariable("couponId") Long couponId,
                                          @Valid @RequestBody UseCouponRequestDto requestDto) {

        log.info("쿠폰 사용 처리 시도 - couponId: {}, orderId: {}, userId: {}", couponId, requestDto.getOrderId(), userId);
        couponService.useCoupon(couponId, requestDto.getOrderId(), userId);

        return ResponseEntity.ok().build();
    }

    @PutMapping("/coupons/order/{orderId}/cancel")
    public ResponseEntity<Void> cancelCouponUsage(@PathVariable("orderId") Long orderId) {
        log.info("쿠폰 사용 취소 시도 - orderId: {}", orderId);
        couponService.cancelCouponUsage(orderId);

        return ResponseEntity.ok().build();
    }
}
