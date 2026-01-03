package com.example.book2onandoncouponservice.controller;


import com.example.book2onandoncouponservice.dto.response.CouponResponseDto;
import com.example.book2onandoncouponservice.service.CouponService;
import com.example.book2onandoncouponservice.service.impl.CouponIssueService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/coupons")
public class CouponController {

    private final CouponService couponService;
    private final CouponIssueService couponIssueService;

    //사용자 조회용
    @GetMapping
    public ResponseEntity<Page<CouponResponseDto>> availableCoupon(Pageable pageable) {

        Page<CouponResponseDto> coupons = couponService.getAvailableCoupon(pageable);
        return ResponseEntity.ok(coupons);
    }

    @GetMapping("/{coupon-id}")
    public ResponseEntity<CouponResponseDto> getCoupon(
            @PathVariable("coupon-id") Long couponId) {

        CouponResponseDto coupon = couponService.getCouponDetail(couponId);

        return ResponseEntity.ok(coupon);
    }

    @GetMapping("/issuable")
    public ResponseEntity<List<CouponResponseDto>> getIssuableCoupons(
            @RequestHeader(value = "X-USER-ID", required = false) Long userId,
            @RequestParam Long bookId,
            @RequestParam List<Long> categoryIds) {

        return ResponseEntity.ok(couponService.getIssuableCoupons(userId, bookId, categoryIds));
    }

    @PostMapping("/{coupon-id}")
    public ResponseEntity<String> issueCoupon(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("coupon-id") Long couponId) {

        boolean isIssuedImmediately = couponIssueService.issueRequest(userId, couponId);

        if (isIssuedImmediately) {
            return ResponseEntity.status(HttpStatus.CREATED).body("쿠폰이 발급되었습니다.");
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED).body("쿠폰 발급 요청이 접수되었습니다. 잠시 후 보관함을 확인해주세요.");
    }

}
