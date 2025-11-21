package com.example.book2onandoncouponservice.service;


import com.example.book2onandoncouponservice.dto.response.MemberCouponResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MemberCouponService {

    //보유 쿠폰 목록 조회
    public Page<MemberCouponResponseDto> getMyCoupon(Long userId, Pageable pageable);

    //쿠폰 사용
    void useMemberCoupon(Long memberCouponId, Long userId);

    //쿠폰 사용 취소
    void cancelMemberCoupon(Long memberCouponId, Long userId);
}
