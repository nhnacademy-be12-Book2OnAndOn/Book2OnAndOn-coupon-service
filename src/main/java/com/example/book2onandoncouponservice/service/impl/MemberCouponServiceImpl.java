package com.example.book2onandoncouponservice.service.impl;

import com.example.book2onandoncouponservice.dto.response.MemberCouponResponseDto;
import com.example.book2onandoncouponservice.entity.MemberCoupon;
import com.example.book2onandoncouponservice.entity.MemberCouponStatus;
import com.example.book2onandoncouponservice.exception.CouponErrorCode;
import com.example.book2onandoncouponservice.exception.CouponIssueException;
import com.example.book2onandoncouponservice.exception.CouponNotFoundException;
import com.example.book2onandoncouponservice.repository.MemberCouponRepository;
import com.example.book2onandoncouponservice.service.MemberCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Slf4j
public class MemberCouponServiceImpl implements MemberCouponService {

    private final MemberCouponRepository memberCouponRepository;

    @Transactional(readOnly = true)
    @Override
    public Page<MemberCouponResponseDto> getMyCoupon(Long userId, Pageable pageable, String status) {

        MemberCouponStatus searchStatus = null;

        if (status != null && !status.isEmpty() && !status.equals("ALL")) {
            try {
                searchStatus = MemberCouponStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
            }
        }

        Page<MemberCoupon> myCoupons = memberCouponRepository.findCouponsWithPolicy(userId, searchStatus, pageable);

        return myCoupons.map(MemberCouponResponseDto::new);
    }

    @Transactional
    @Override
    public void useMemberCoupon(Long memberCouponId, Long userId, Long orderId) {

        MemberCoupon memberCoupon = memberCouponRepository.findById(memberCouponId)
                .orElseThrow(CouponNotFoundException::new);

        if (!memberCoupon.getUserId().equals(userId)) {
            throw new CouponIssueException(CouponErrorCode.NOT_COUPON_OWNER);
        }

        memberCoupon.use(orderId);
        log.info("쿠폰 사용 : orderId: {}, couponId: {}", orderId, memberCoupon.getMemberCouponId());
    }

    @Transactional
    @Override
    public void cancelMemberCoupon(Long orderId) {

        MemberCoupon memberCoupon = memberCouponRepository.findByOrderId(orderId)
                .orElseThrow(CouponNotFoundException::new);

        if (memberCoupon.getOrderId() != null && !memberCoupon.getOrderId().equals(orderId)) {
            throw new CouponIssueException(CouponErrorCode.INVALID_COUPON_ORDER_MATCH);
        }

        memberCoupon.cancelUsage();
        log.info("주문 취소로 인한 쿠폰 복구 완료: orderId={}, couponId={}", orderId, memberCoupon.getMemberCouponId());
    }
}
