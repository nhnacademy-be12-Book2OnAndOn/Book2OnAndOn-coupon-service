package com.example.book2onandoncouponservice.service;


import com.example.book2onandoncouponservice.dto.request.OrderCouponCheckRequestDto;
import com.example.book2onandoncouponservice.dto.response.CouponTargetResponseDto;
import com.example.book2onandoncouponservice.dto.response.MemberCouponResponseDto;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public interface MemberCouponService {

    @Transactional(readOnly = true)
    Page<MemberCouponResponseDto> getMyCoupon(Long userId, Pageable pageable, String status);

    //쿠폰 사용
    void useMemberCoupon(Long memberCouponId, Long userId, String orderNumber);

    //쿠폰 사용 취소
    void cancelMemberCoupon(String orderNumber);

    List<MemberCouponResponseDto> getUsableCoupons(Long userId, OrderCouponCheckRequestDto requestDto);

    @Transactional(readOnly = true)
    CouponTargetResponseDto getCouponTargets(Long memberCouponId);
}