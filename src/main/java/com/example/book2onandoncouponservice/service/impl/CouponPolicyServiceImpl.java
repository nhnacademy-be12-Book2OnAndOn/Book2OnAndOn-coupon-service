package com.example.book2onandoncouponservice.service.impl;

import com.example.book2onandoncouponservice.dto.request.CouponPolicyRequestDto;
import com.example.book2onandoncouponservice.dto.request.CouponPolicyUpdateRequestDto;
import com.example.book2onandoncouponservice.dto.response.CouponPolicyResponseDto;
import com.example.book2onandoncouponservice.entity.CouponPolicy;
import com.example.book2onandoncouponservice.entity.CouponPolicyDiscountType;
import com.example.book2onandoncouponservice.entity.CouponPolicyStatus;
import com.example.book2onandoncouponservice.entity.CouponPolicyTargetBook;
import com.example.book2onandoncouponservice.entity.CouponPolicyTargetCategory;
import com.example.book2onandoncouponservice.entity.CouponPolicyType;
import com.example.book2onandoncouponservice.exception.CouponPolicyNotFoundException;
import com.example.book2onandoncouponservice.repository.CouponPolicyRepository;
import com.example.book2onandoncouponservice.repository.CouponPolicyTargetBookRepository;
import com.example.book2onandoncouponservice.repository.CouponPolicyTargetCategoryRepository;
import com.example.book2onandoncouponservice.service.CouponPolicyService;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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

        log.info("쿠폰 정책 목록 조회 요청. type={}, discountType={}, status={}, page={}",
                type, discountType, status, pageable.getPageNumber());

        Page<CouponPolicy> policies = couponPolicyRepository.findAllByFilters(type, discountType, status, pageable);

        log.info("쿠폰 정책 목록 조회 완료. totalElements={}", policies.getTotalElements());
        return policies.map(policy ->
                new CouponPolicyResponseDto(policy, List.of(), List.of())
        );
    }

    //특정 쿠폰정책 조회
    @Override
    public CouponPolicyResponseDto getCouponPolicy(Long couponPolicyId) {

        log.info("쿠폰 정책 상세 조회 요청. policyId={}", couponPolicyId);

        CouponPolicy couponPolicy = couponPolicyRepository.findById(couponPolicyId)
                .orElseThrow(() -> {
                    log.error("쿠폰 정책 상세 조회 실패 - 존재하지 않는 ID. policyId={}", couponPolicyId);
                    return new CouponPolicyNotFoundException();
                });

        List<Long> bookIds = targetBookRepository.findAllByCouponPolicy_CouponPolicyId(couponPolicyId)
                .stream()
                .map(CouponPolicyTargetBook::getBookId)
                .toList();

        List<Long> categoryIds = targetCategoryRepository.findAllByCouponPolicy_CouponPolicyId(couponPolicyId)
                .stream()
                .map(CouponPolicyTargetCategory::getCategoryId)
                .toList();

        log.info("쿠폰 정책 상세 조회 완료. policyId={}, targetBooksCount={}, targetCategoriesCount={}",
                couponPolicyId, bookIds.size(), categoryIds.size());

        return new CouponPolicyResponseDto(couponPolicy, bookIds, categoryIds);
    }


    //쿠폰정책 생성
    @Transactional
    @Override
    public Long createPolicy(CouponPolicyRequestDto requestDto) {

        log.info("쿠폰 정책 생성 요청. name={}, type={}, discountType={}",
                requestDto.getCouponPolicyName(), requestDto.getCouponPolicyType(),
                requestDto.getCouponPolicyDiscountType());

        CouponPolicy policy = new CouponPolicy(requestDto);
        CouponPolicy savedPolicy = couponPolicyRepository.save(policy);

        saveTargetBooks(savedPolicy, requestDto.getTargetBookIds());
        saveTargetCategories(savedPolicy, requestDto.getTargetCategoryIds());

        log.info("쿠폰 정책 생성 완료. policyId={}", savedPolicy.getCouponPolicyId());
        return savedPolicy.getCouponPolicyId();
    }


    //쿠폰정책 업데이트
    @Transactional
    @Override
    public void updatePolicy(Long couponPolicyId, CouponPolicyUpdateRequestDto requestDto) {

        log.info("쿠폰 정책 수정 요청. policyId={}", couponPolicyId);

        CouponPolicy couponPolicy = couponPolicyRepository.findById(couponPolicyId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 쿠폰정책입니다."));

        couponPolicy.updatePolicy(requestDto);

        boolean removeTargetBook = requestDto.getRemoveTargetBook();
        boolean removeTargetCategory = requestDto.getRemoveTargetCategory();

        if (removeTargetBook) {
            log.info("타겟 도서 전체 삭제 요청. policyId={}", couponPolicyId);
            targetBookRepository.deleteByCouponPolicy_CouponPolicyId(couponPolicyId);
        } else if (requestDto.getTargetBookIds() != null) {  // 값이 있을 때만 저장
            log.info("타겟 도서 목록 갱신. policyId={}, count={}", couponPolicyId, requestDto.getTargetBookIds().size());
            targetBookRepository.deleteByCouponPolicy_CouponPolicyId(couponPolicyId);
            saveTargetBooks(couponPolicy, requestDto.getTargetBookIds());
        }

        if (removeTargetCategory) {
            log.info("타겟 카테고리 전체 삭제 요청. policyId={}", couponPolicyId);
            targetCategoryRepository.deleteByCouponPolicy_CouponPolicyId(couponPolicyId);
        } else if (requestDto.getTargetCategoryIds() != null) {
            log.info("타겟 카테고리 목록 갱신. policyId={}, count={}", couponPolicyId, requestDto.getTargetCategoryIds().size());
            targetCategoryRepository.deleteByCouponPolicy_CouponPolicyId(couponPolicyId);
            saveTargetCategories(couponPolicy, requestDto.getTargetCategoryIds());
        }
        log.info("쿠폰 정책 수정 완료. policyId={}", couponPolicyId);
    }

    //쿠폰정책 비활성화
    @Transactional
    @Override
    public void deactivatePolicy(Long couponPolicyId) {

        log.info("쿠폰 정책 비활성화 요청. policyId={}", couponPolicyId);

        CouponPolicy couponPolicy = couponPolicyRepository.findById(couponPolicyId)
                .orElseThrow(() -> {
                    log.error("쿠폰 정책 비활성화 실패 - 존재하지 않는 ID. policyId={}", couponPolicyId);
                    return new CouponPolicyNotFoundException();
                });

        couponPolicy.deActive();
        log.info("쿠폰 정책 비활성화 완료. policyId={}", couponPolicyId);
    }


    //쿠폰정책 생성 시 리스트로 받은 bookId를 이용해 CouponPolicyTargetBook 생성
    private void saveTargetBooks(CouponPolicy couponPolicy, List<Long> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) {
            return;
        }

        log.debug("타겟 도서 저장 시도. policyId={}, bookIdsCount={}", couponPolicy.getCouponPolicyId(), bookIds.size());

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

        log.debug("타겟 카테고리 저장 시도. policyId={}, categoryIdsCount={}", couponPolicy.getCouponPolicyId(),
                categoryIds.size());

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
