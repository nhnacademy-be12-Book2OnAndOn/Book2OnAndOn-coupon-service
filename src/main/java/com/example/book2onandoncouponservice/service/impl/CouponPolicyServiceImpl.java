package com.example.book2onandoncouponservice.service.impl;

import com.example.book2onandoncouponservice.dto.request.CouponPolicyRequestDto;
import com.example.book2onandoncouponservice.dto.response.CouponPolicyResponseDto;
import com.example.book2onandoncouponservice.entity.CouponPolicy;
import com.example.book2onandoncouponservice.entity.CouponPolicyDiscountType;
import com.example.book2onandoncouponservice.entity.CouponPolicyStatus;
import com.example.book2onandoncouponservice.entity.CouponPolicyTargetBook;
import com.example.book2onandoncouponservice.entity.CouponPolicyTargetCategory;
import com.example.book2onandoncouponservice.entity.CouponPolicyType;
import com.example.book2onandoncouponservice.repository.CouponPolicyRepository;
import com.example.book2onandoncouponservice.repository.CouponPolicyTargetBookRepository;
import com.example.book2onandoncouponservice.repository.CouponPolicyTargetCategoryRepository;
import com.example.book2onandoncouponservice.service.CouponPolicyService;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class CouponPolicyServiceImpl implements CouponPolicyService {
    private final CouponPolicyRepository couponPolicyRepository;
    private final CouponPolicyTargetBookRepository targetBookRepository;
    private final CouponPolicyTargetCategoryRepository targetCategoryRepository;

    //쿠폰정책 조회 Pageable
    @Transactional(readOnly = true)
    @Override
    public Page<CouponPolicyResponseDto> getCouponPolicies(
            CouponPolicyType type,
            CouponPolicyDiscountType discountType,
            CouponPolicyStatus status,
            Pageable pageable) {

        Page<CouponPolicy> policies = couponPolicyRepository.findAllByFilters(type, discountType, status, pageable);

        return policies.map(policy ->
                new CouponPolicyResponseDto(policy, List.of(), List.of())
        );
    }

    //특정 쿠폰정책 조회
    @Override
    public CouponPolicyResponseDto getCouponPolicy(Long couponPolicyId) {

        CouponPolicy couponPolicy = couponPolicyRepository.findById(couponPolicyId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 쿠폰정책입니다."));

        List<Long> bookIds = targetBookRepository.findAllByCouponPolicy_CouponPolicyId(couponPolicyId)
                .stream()
                .map(CouponPolicyTargetBook::getBookId)
                .toList();

        List<Long> categoryIds = targetCategoryRepository.findAllByCouponPolicy_CouponPolicyId(couponPolicyId)
                .stream()
                .map(CouponPolicyTargetCategory::getCategoryId)
                .toList();

        return new CouponPolicyResponseDto(couponPolicy, bookIds, categoryIds);
    }


    //쿠폰정책 생성
    @Transactional
    @Override
    public Long createPolicy(CouponPolicyRequestDto requestDto) {
        if (couponPolicyRepository.existsByCouponPolicyName(requestDto.getCouponPolicyName())) {
            throw new IllegalStateException("이미 존재하는 쿠폰 정책 이름입니다.");
        }

        CouponPolicy policy = new CouponPolicy(requestDto);
        CouponPolicy savedPolicy = couponPolicyRepository.save(policy);

        saveTargetBooks(savedPolicy, requestDto.getTargetBookIds());
        saveTargetCategories(savedPolicy, requestDto.getTargetCategoryIds());

        return savedPolicy.getCouponPolicyId();
    }


    //쿠폰정책 업데이트
    @Transactional
    @Override
    public void updatePolicy(Long couponPolicyId, CouponPolicyRequestDto requestDto) {
        CouponPolicy couponPolicy = couponPolicyRepository.findById(couponPolicyId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 쿠폰정책입니다."));

        couponPolicy.updatePolicy(requestDto);

        targetBookRepository.deleteByCouponPolicy_CouponPolicyId(couponPolicyId);
        targetCategoryRepository.deleteByCouponPolicy_CouponPolicyId(couponPolicyId);

        saveTargetBooks(couponPolicy, requestDto.getTargetBookIds());
        saveTargetCategories(couponPolicy, requestDto.getTargetCategoryIds());
    }

    //쿠폰정책 비활성화
    @Transactional
    @Override
    public void deactivatePolicy(Long couponPolicyId) {
        CouponPolicy couponPolicy = couponPolicyRepository.findById(couponPolicyId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 쿠폰정책입니다."));

        couponPolicy.deActive();
    }


    //쿠폰정책 생성 시 리스트로 받은 bookId를 이용해 CouponPolicyTargetBook 생성
    private void saveTargetBooks(CouponPolicy couponPolicy, List<Long> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) {
            return;
        }
        List<CouponPolicyTargetBook> targets = bookIds.stream()
                .filter(Objects::nonNull)
                .map(bookId -> CouponPolicyTargetBook.builder()
                        .couponPolicy(couponPolicy)
                        .bookId(bookId)
                        .build())
                .toList();

        if (!targets.isEmpty()) {
            targetBookRepository.saveAll(targets);
        }
    }

    //쿠폰정책 생성 시 리스트로 받은 categoryId를 이용해 CouponPolicyTargetCategory 생성
    private void saveTargetCategories(CouponPolicy couponPolicy, List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return;
        }

        List<CouponPolicyTargetCategory> targets = categoryIds.stream()
                .filter(Objects::nonNull)
                .map(categoryId -> CouponPolicyTargetCategory.builder()
                        .couponPolicy(couponPolicy)
                        .categoryId(categoryId)
                        .build())
                .toList();

        if (!targets.isEmpty()) {
            targetCategoryRepository.saveAll(targets);
        }
    }
}
