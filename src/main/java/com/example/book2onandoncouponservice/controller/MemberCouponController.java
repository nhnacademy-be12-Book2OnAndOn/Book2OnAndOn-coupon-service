package com.example.book2onandoncouponservice.controller;

import com.example.book2onandoncouponservice.dto.request.OrderCouponCheckRequestDto;
import com.example.book2onandoncouponservice.dto.request.UseCouponRequestDto;
import com.example.book2onandoncouponservice.dto.response.CouponTargetResponseDto;
import com.example.book2onandoncouponservice.dto.response.MemberCouponResponseDto;
import com.example.book2onandoncouponservice.service.MemberCouponService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/my-coupon")
public class MemberCouponController {
    private final MemberCouponService memberCouponService;

    @PostMapping("/{member-coupon-id}/use")
    public ResponseEntity<Void> useCoupon(@PathVariable("member-coupon-id") Long memberCouponId,
                                          @RequestHeader("X-USER-ID") Long userId,
                                          @RequestBody UseCouponRequestDto requestDto) {
        memberCouponService.useMemberCoupon(memberCouponId, userId, requestDto.getOrderId());
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

    //주문 사용 가능용
    @PostMapping("/usable")
    public ResponseEntity<List<MemberCouponResponseDto>> getUsableCoupons(
            @RequestHeader("X-USER-ID") Long userId,
            @Valid @RequestBody OrderCouponCheckRequestDto requestDto) {

        List<MemberCouponResponseDto> result = memberCouponService.getUsableCoupons(
                userId,
                requestDto);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{member-coupon-id}/targets")
    public ResponseEntity<CouponTargetResponseDto> getCouponTargets(
            @PathVariable("member-coupon-id") Long memberCouponId) {

        CouponTargetResponseDto response = memberCouponService.getCouponTargets(memberCouponId);

        return ResponseEntity.ok(response);
    }
}
