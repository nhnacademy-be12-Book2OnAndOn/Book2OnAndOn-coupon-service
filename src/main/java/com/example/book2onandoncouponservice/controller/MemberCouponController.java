package com.example.book2onandoncouponservice.controller;

import com.example.book2onandoncouponservice.dto.response.MemberCouponResponseDto;
import com.example.book2onandoncouponservice.service.MemberCouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/my-coupon")
public class MemberCouponController {
    private final MemberCouponService memberCouponService;

    @PostMapping("/{memberCouponId}/use")
    public ResponseEntity<Void> useCoupon(@PathVariable Long memberCouponId,
                                          @RequestHeader("X-USER-ID") Long userId) {
        memberCouponService.useMemberCoupon(memberCouponId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<Page<MemberCouponResponseDto>> getMyCoupons(
            @RequestHeader("X-USER-ID") Long userId,
            Pageable pageable,
            @RequestParam(required = false) String status) {

        Page<MemberCouponResponseDto> coupons = memberCouponService.getMyCoupon(userId, pageable, status);

        return ResponseEntity.ok(coupons);
    }

    @PostMapping("/{memberCouponId}/cancel")
    public ResponseEntity<Void> cancelCoupon(
            @PathVariable Long memberCouponId,
            @RequestHeader("X-USER-ID") Long userId) {
        memberCouponService.cancelMemberCoupon(memberCouponId, userId);
        return ResponseEntity.ok().build();
    }
}
