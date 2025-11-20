package com.example.book2onandoncouponservice.controller;


import com.example.book2onandoncouponservice.dto.request.CouponCreateRequestDto;
import com.example.book2onandoncouponservice.dto.response.CouponResponseDto;
import com.example.book2onandoncouponservice.repository.CouponRepository;
import com.example.book2onandoncouponservice.service.CouponPolicyService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
public class CouponController {

    private final CouponService couponService;
    private final CouponPolicyService couponPolicyService; // 정책 관련 API를 위해 추가
    private final CouponRepository couponRepository;

    @GetMapping("/admin/coupons")
    public ResponseEntity<Page<CouponResponseDto>> getCoupons(
            Pageable pageable) {

        Page<CouponResponseDto> coupons = couponService.getCoupons(pageable);

        return ResponseEntity.ok(coupons);
    }


    @PostMapping("/admin/coupons")
    public ResponseEntity<Void> createCoupon(
            @Valid @RequestBody CouponCreateRequestDto requestDto) {

        Long couponId = couponService.createCouponUnit(requestDto);
        log.info("쿠폰 생성 완료: {} ", couponId);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    //사용자 조회용
    @GetMapping("/coupons")
    public ResponseEntity<Page<CouponResponseDto>> availableCoupon(Pageable pageable) {

        Page<CouponResponseDto> coupons = couponService.getAvailableCoupon(pageable);
        return ResponseEntity.ok(coupons);
    }

    @GetMapping("/coupons/{couponId}")
    public ResponseEntity<CouponResponseDto> getCoupon(
            @PathVariable("couponId") Long couponId) {

        CouponResponseDto coupon = couponService.getCouponDetail(couponId);

        return ResponseEntity.ok(coupon);
    }

    @PostMapping("/coupons/{couponId}")
    public ResponseEntity<Void> issueCoupon(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("couponId") Long couponId) {

        Long memberCouponId = couponService.issueMemberCoupon(userId, couponId);
        log.info("사용자 발급 완료: UserId={}, MemberCouponId={}", userId, memberCouponId);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }


}
