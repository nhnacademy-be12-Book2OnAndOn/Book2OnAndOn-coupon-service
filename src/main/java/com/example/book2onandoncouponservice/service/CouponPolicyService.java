package com.example.book2onandoncouponservice.service;

import com.example.book2onandoncouponservice.dto.request.CouponPolicyRequestDto;
import com.example.book2onandoncouponservice.dto.response.CouponPolicyResponseDto;
import com.example.book2onandoncouponservice.entity.CouponPolicyDiscountType;
import com.example.book2onandoncouponservice.entity.CouponPolicyStatus;
import com.example.book2onandoncouponservice.entity.CouponPolicyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CouponPolicyService {

    //쿠폰 정책 목록 조회
    Page<CouponPolicyResponseDto> getCouponPolicies(CouponPolicyType type,
                                                    CouponPolicyDiscountType discountType,
                                                    CouponPolicyStatus status,
                                                    Pageable pageable);

    //특정 쿠폰 정책 조회
    CouponPolicyResponseDto getCouponPolicy(Long couponPolicyId);

    //쿠폰 정책 생성
    Long createPolicy(CouponPolicyRequestDto requestDto);

    //쿠폰 정책 수정
    void updatePolicy(Long couponPolicyId, CouponPolicyRequestDto requestDto);

    //쿠폰 정책 비활성화
    void deactivatePolicy(Long couponPolicyId);
}
