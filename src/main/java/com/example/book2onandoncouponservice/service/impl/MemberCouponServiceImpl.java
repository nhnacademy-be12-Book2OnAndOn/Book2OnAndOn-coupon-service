package com.example.book2onandoncouponservice.service.impl;

import com.example.book2onandoncouponservice.dto.response.MemberCouponResponseDto;
import com.example.book2onandoncouponservice.entity.MemberCoupon;
import com.example.book2onandoncouponservice.repository.MemberCouponRepository;
import com.example.book2onandoncouponservice.service.MemberCouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class MemberCouponServiceImpl implements MemberCouponService {

    private final MemberCouponRepository memberCouponRepository;

    @Transactional(readOnly = true)
    @Override
    public Page<MemberCouponResponseDto> getMyCoupon(Long userId, Pageable pageable) {

        Page<MemberCoupon> myCoupons = memberCouponRepository.findCouponsWithPolicy(userId, pageable);

        return myCoupons.map(MemberCouponResponseDto::new);
    }

    @Transactional
    @Override
    public void useMemberCoupon(Long memberCouponId, Long userId) {

        MemberCoupon memberCoupon = memberCouponRepository.findById(memberCouponId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자 쿠폰입니다."));

        if (!memberCoupon.getUserId().equals(userId)) {
            throw new RuntimeException("해당 쿠폰의 소유자가 아닙니다.");
        }

        memberCoupon.use();
    }

    @Transactional
    @Override
    public void cancelMemberCoupon(Long memberCouponId, Long userId) {

        MemberCoupon memberCoupon = memberCouponRepository.findById(memberCouponId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자 쿠폰입니다."));
        memberCoupon.cancelUsage();
    }
}
