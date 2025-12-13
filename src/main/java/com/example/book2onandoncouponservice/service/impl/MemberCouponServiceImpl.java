package com.example.book2onandoncouponservice.service.impl;

import com.example.book2onandoncouponservice.dto.request.OrderCouponCheckRequestDto;
import com.example.book2onandoncouponservice.dto.response.CouponTargetResponseDto;
import com.example.book2onandoncouponservice.dto.response.MemberCouponResponseDto;
import com.example.book2onandoncouponservice.entity.CouponPolicy;
import com.example.book2onandoncouponservice.entity.CouponPolicyTargetBook;
import com.example.book2onandoncouponservice.entity.CouponPolicyTargetCategory;
import com.example.book2onandoncouponservice.entity.MemberCoupon;
import com.example.book2onandoncouponservice.entity.MemberCouponStatus;
import com.example.book2onandoncouponservice.exception.CouponErrorCode;
import com.example.book2onandoncouponservice.exception.CouponIssueException;
import com.example.book2onandoncouponservice.exception.CouponNotFoundException;
import com.example.book2onandoncouponservice.repository.CouponPolicyRepository;
import com.example.book2onandoncouponservice.repository.MemberCouponRepository;
import com.example.book2onandoncouponservice.service.MemberCouponService;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
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
    private final CouponPolicyRepository couponPolicyRepository;

    @Transactional(readOnly = true)
    @Override
    public Page<MemberCouponResponseDto> getMyCoupon(Long userId, Pageable pageable, String status) {

        log.info("내 쿠폰 목록 조회 요청. userId={}, status={}, page={}", userId, status, pageable.getPageNumber());

        MemberCouponStatus searchStatus = null;

        if (status != null && !status.isEmpty() && !status.equals("ALL")) {
            try {
                searchStatus = MemberCouponStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                log.warn("유효하지 않은 쿠폰 상태 검색어: {}", status);
            }
        }

        Page<MemberCoupon> myCoupons = memberCouponRepository.findCouponsWithPolicy(userId, searchStatus, pageable);

        log.info("내 쿠폰 목록 조회 완료. totalElements={}", myCoupons.getTotalElements());
        return myCoupons.map(MemberCouponResponseDto::new);
    }

    @Transactional
    @Override
    public void useMemberCoupon(Long memberCouponId, Long userId, Long orderId) {
        log.info("쿠폰 사용 요청. memberCouponId={}, userId={}, orderId={}", memberCouponId, userId, orderId);

        MemberCoupon memberCoupon = memberCouponRepository.findById(memberCouponId)
                .orElseThrow(() -> {
                    log.error("쿠폰 사용 실패: 존재하지 않는 쿠폰. memberCouponId={}", memberCouponId);
                    return new CouponNotFoundException();
                });

        if (!memberCoupon.getUserId().equals(userId)) {
            log.warn("쿠폰 사용 실패: 소유자 불일치. reqUserId={}, ownerId={}", userId, memberCoupon.getUserId());
            throw new CouponIssueException(CouponErrorCode.NOT_COUPON_OWNER);
        }

        memberCoupon.use(orderId);
        log.info("쿠폰 사용 : orderId: {}, couponId: {}", orderId, memberCoupon.getMemberCouponId());
    }

    @Transactional
    @Override
    public void cancelMemberCoupon(Long orderId) {

        log.info("쿠폰 사용 취소(롤백) 요청. orderId={}", orderId);

        MemberCoupon memberCoupon = memberCouponRepository.findByOrderId(orderId)
                .orElseThrow(() -> {
                    log.warn("쿠폰 취소 실패: 해당 주문에 사용된 쿠폰 없음. orderId={}", orderId);
                    return new CouponNotFoundException();
                });

        if (memberCoupon.getOrderId() != null && !memberCoupon.getOrderId().equals(orderId)) {
            log.warn("쿠폰 취소 실패: 주문 번호 불일치. reqOrderId={}, couponOrderId={}", orderId, memberCoupon.getOrderId());
            throw new CouponIssueException(CouponErrorCode.INVALID_COUPON_ORDER_MATCH);
        }

        memberCoupon.cancelUsage();
        log.info("주문 취소로 인한 쿠폰 복구 완료: orderId={}, couponId={}", orderId, memberCoupon.getMemberCouponId());
    }


    // 특정 주문에 사용 가능한 쿠폰 조회
    @Transactional(readOnly = true)
    @Override
    public List<MemberCouponResponseDto> getUsableCoupons(Long userId, OrderCouponCheckRequestDto requestDto) {
        int bookCount = requestDto.getBookIds() != null ? requestDto.getBookIds().size() : 0;
        int categoryCount = requestDto.getCategoryIds() != null ? requestDto.getCategoryIds().size() : 0;

        log.info("주문 적용 가능 쿠폰 조회 요청. userId={}, bookIdsCount={}, categoryIdsCount={}",
                userId, bookCount, categoryCount);

        List<Long> couponPolicyIds = couponPolicyRepository.findApplicablePolicyIds(
                requestDto.getBookIds(),
                requestDto.getCategoryIds());

        if (couponPolicyIds.isEmpty()) {
            log.info("사용 가능한 쿠폰이 없습니다. userId:{}", userId);
            return Collections.emptyList();
        }
        log.debug("매칭된 정책 ID 개수: {}", couponPolicyIds.size());

        List<MemberCoupon> usableCoupons = memberCouponRepository.findUsableCouponsByPolicyIds(
                userId,
                couponPolicyIds,
                LocalDateTime.now());

        log.info("해당 주문에 사용 가능한 쿠폰 개수 조회 성공: {}", usableCoupons.size());

        return usableCoupons.stream()
                .map(MemberCouponResponseDto::new)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public CouponTargetResponseDto getCouponTargets(Long memberCouponId) {

        log.info("쿠폰 적용 대상(Target) 조회 요청. memberCouponId={}", memberCouponId);

        MemberCoupon memberCoupon = memberCouponRepository.findByIdWithTargets(memberCouponId)
                .orElseThrow(() -> {
                    log.warn("쿠폰 조회 실패: 존재하지 않는 쿠폰입니다. memberCouponId={}", memberCouponId);
                    return new CouponNotFoundException();
                });

        CouponPolicy policy = memberCoupon.getCoupon().getCouponPolicy();

        // 타겟 도서 ID 추출
        List<Long> bookIds = (policy.getCouponPolicyTargetBooks() != null)
                ? policy.getCouponPolicyTargetBooks().stream()
                .map(CouponPolicyTargetBook::getBookId)
                .toList()
                : List.of();

        // 타겟 카테고리 ID 추출
        List<Long> categoryIds = (policy.getCouponPolicyTargetCategories() != null)
                ? policy.getCouponPolicyTargetCategories().stream()
                .map(CouponPolicyTargetCategory::getCategoryId)
                .toList()
                : List.of();

        CouponTargetResponseDto response = CouponTargetResponseDto.builder()
                .memberCouponId(memberCoupon.getMemberCouponId())
                .targetBookIds(bookIds)
                .targetCategoryIds(categoryIds)
                .minPrice(policy.getMinPrice())
                .maxPrice(policy.getMaxPrice())
                .discountType(policy.getCouponPolicyDiscountType())
                .discountValue(policy.getCouponDiscountValue())
                .build();

        log.info("쿠폰 적용 대상 조회 완료. memberCouponId={}, bookIdsSize={}, categoryIdsSize={}", memberCouponId,
                bookIds.size(), categoryIds.size());

        return response;
    }
}
