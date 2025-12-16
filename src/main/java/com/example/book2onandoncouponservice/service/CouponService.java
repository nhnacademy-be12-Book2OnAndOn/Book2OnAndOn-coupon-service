package com.example.book2onandoncouponservice.service;

import com.example.book2onandoncouponservice.dto.request.CouponCreateRequestDto;
import com.example.book2onandoncouponservice.dto.response.CouponResponseDto;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public interface CouponService {

    //쿠폰 생성
    Long createCouponUnit(CouponCreateRequestDto requestDto);

    //관리자용 모든 쿠폰 목록 조회
    Page<CouponResponseDto> getCoupons(Pageable pageable, String status);

    //특정 쿠폰 조회
    CouponResponseDto getCouponDetail(Long couponUnitId);

    //사용자용 발급가능한 쿠폰 목록 조회
    Page<CouponResponseDto> getAvailableCoupon(Pageable pageable);

    //사용자 쿠폰 발급
    Long issueMemberCoupon(Long userId, Long couponUnitId);

    //쿠폰 수량 업데이트
    Integer updateAccount(Long couponId, Integer account);

    //웰컴쿠폰 지급
    void issueWelcomeCoupon(Long userId);

    //생일쿠폰 지급
    void issueBirthdayCoupon(Long userId);

    //적용가능한 쿠폰 확인
    @Transactional(readOnly = true)
    List<CouponResponseDto> getAppliableCoupons(Long bookId, List<Long> categoryIds);
}