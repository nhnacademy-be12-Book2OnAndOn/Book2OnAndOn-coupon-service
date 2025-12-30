package com.example.book2onandoncouponservice.service.impl;

import com.example.book2onandoncouponservice.dto.request.CouponCreateRequestDto;
import com.example.book2onandoncouponservice.dto.response.CouponResponseDto;
import com.example.book2onandoncouponservice.entity.Coupon;
import com.example.book2onandoncouponservice.entity.CouponPolicy;
import com.example.book2onandoncouponservice.entity.CouponPolicyStatus;
import com.example.book2onandoncouponservice.entity.CouponPolicyType;
import com.example.book2onandoncouponservice.entity.MemberCoupon;
import com.example.book2onandoncouponservice.exception.CouponErrorCode;
import com.example.book2onandoncouponservice.exception.CouponIssueException;
import com.example.book2onandoncouponservice.exception.CouponNotFoundException;
import com.example.book2onandoncouponservice.exception.CouponPolicyNotFoundException;
import com.example.book2onandoncouponservice.repository.CouponPolicyRepository;
import com.example.book2onandoncouponservice.repository.CouponRepository;
import com.example.book2onandoncouponservice.repository.MemberCouponRepository;
import com.example.book2onandoncouponservice.service.CouponService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@RequiredArgsConstructor
@Service
public class CouponServiceImpl implements CouponService {

    private final CouponPolicyRepository policyRepository;
    private final CouponRepository couponRepository;
    private final MemberCouponRepository memberCouponRepository;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    @Override
    public Long createCouponUnit(CouponCreateRequestDto requestDto) {

        log.info("쿠폰 생성 요청. policyId={}, quantity={}", requestDto.getCouponPolicyId(),
                requestDto.getCouponRemainingQuantity());
        CouponPolicy policy = policyRepository.findById(requestDto.getCouponPolicyId())
                .orElseThrow(() -> {
                    log.error("쿠폰 정책을 찾을 수 없음. policyId={}", requestDto.getCouponPolicyId());
                    return new CouponPolicyNotFoundException();
                });

        if (policy.getCouponPolicyStatus() == CouponPolicyStatus.DEACTIVE) {
            log.warn("발급 불가능한 정책으로 쿠폰 생성 시도. policyId={}", policy.getCouponPolicyId());
            throw new CouponIssueException(CouponErrorCode.POLICY_NOT_ISSUABLE);
        }

        Coupon coupon = new Coupon(requestDto.getCouponRemainingQuantity(), policy);
        Coupon savedCoupon = couponRepository.save(coupon);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                String redisKey = "coupon:" + savedCoupon.getCouponId() + "stock:";
                String initialQuantity = (savedCoupon.getCouponRemainingQuantity() == null)
                        ? String.valueOf(Long.MAX_VALUE)
                        : String.valueOf(savedCoupon.getCouponRemainingQuantity());

                redisTemplate.opsForValue().set(redisKey, initialQuantity);

                log.info("Redis 재고 초기화 완료. key={}, quantity={}", redisKey, initialQuantity);
            }
        });

        log.info("쿠폰 생성 완료. generatedCouponId={}", savedCoupon.getCouponId());
        return savedCoupon.getCouponId();
    }

    // 전체 쿠폰 조회
    @Transactional(readOnly = true)
    @Override
    public Page<CouponResponseDto> getCoupons(Pageable pageable, String status) {

        log.info("전체 쿠폰 목록 조회 요청. page={}, size={}, status={}", pageable.getPageNumber(), pageable.getPageSize(),
                status);
        CouponPolicyStatus policyStatus = null;

        if (status != null && !status.isEmpty() && !status.equals("ALL")) {
            try {
                policyStatus = CouponPolicyStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                log.warn("유효하지 않은 쿠폰 상태 검색어: {}", status);
            }
        }
        Page<Coupon> coupons = couponRepository.findAllByPolicyStatus(policyStatus, pageable);
        log.info("전체 쿠폰 목록 조회 완료. totalElements={}", coupons.getTotalElements());

        return coupons.map(CouponResponseDto::new);
    }


    // 쿠폰 상세 조회
    @Override
    public CouponResponseDto getCouponDetail(Long couponId) {
        log.info("쿠폰 상세 조회 요청. couponId={}", couponId);
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(CouponNotFoundException::new);
        return new CouponResponseDto(coupon);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<CouponResponseDto> getAvailableCoupon(Pageable pageable) {

        log.info("발급 가능(다운로드용) 쿠폰 목록 조회 요청. page={}", pageable.getPageNumber());
        LocalDate today = LocalDate.now();
        Page<Coupon> coupons = couponRepository.findAvailableCoupons(
                CouponPolicyStatus.ACTIVE,
                today,
                pageable
        );
        return coupons.map(CouponResponseDto::new);
    }

    @Transactional
    @Override
    public Long issueMemberCoupon(Long userId, Long couponId) {

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> {
                            log.error("존재하지 않는 쿠폰. couponId={}", couponId);
                            return new CouponNotFoundException();
                        }
                );

        CouponPolicy policy = coupon.getCouponPolicy();

        if (!policy.isIssuable()) {
            log.warn("발급 기간이 아니거나 비활성화된 정책. policyId={}, userId={}", policy.getCouponPolicyId(), userId);
            throw new CouponIssueException(CouponErrorCode.POLICY_NOT_ISSUABLE);
        }

        if (memberCouponRepository.existsByUserIdAndCoupon_CouponId(userId, couponId)) {
            log.warn("이미 발급된 쿠폰. userId={}, couponId={}", userId, couponId);
            throw new CouponIssueException(CouponErrorCode.COUPON_ALREADY_ISSUED);
        }

        if (coupon.getCouponRemainingQuantity() != null) {
            int updatedRows = couponRepository.decreaseRemainingQuantity(couponId);

            if (updatedRows == 0) {
                log.error("DB 재고 차감 실패 - 이미 소진됨. couponId={}", couponId);
                throw new CouponIssueException(CouponErrorCode.COUPON_OUT_OF_STOCK);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = calculateExpirationDate(policy, now);

        MemberCoupon memberCoupon = new MemberCoupon(
                userId,
                coupon,
                now,
                endDate
        );

        MemberCoupon savedMemberCoupon = memberCouponRepository.save(memberCoupon);
        log.info("회원 쿠폰 발급 성공. memberCouponId={}, userId={}, expirationDate={}",
                savedMemberCoupon.getMemberCouponId(), userId, endDate);

        return savedMemberCoupon.getMemberCouponId();
    }

    //쿠폰 수량 업데이트
    @Transactional
    @Override
    public Integer updateAccount(Long couponId, Integer quantity) {

        log.info("쿠폰 수량 변경 요청. couponId={}, newQuantity={}", couponId, quantity);

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(CouponNotFoundException::new);

        coupon.update(quantity);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                String redisKey = "coupon:" + couponId + "stock:";

                String redisValue = (quantity == null)
                        ? String.valueOf(Long.MAX_VALUE)
                        : String.valueOf(quantity);

                redisTemplate.opsForValue().set(redisKey, redisValue);

                log.info("Redis 재고 동기화(수정) 완료. key={}, newQuantity={}", redisKey, redisValue);
            }
        });

        log.info("쿠폰 수량 변경 완료. couponId={}", couponId);
        return quantity;
    }

    //회원가입 시 웰컴쿠폰 지급
    @Transactional
    @Override
    public void issueWelcomeCoupon(Long userId) {
        CouponPolicy welcomePolicy = policyRepository.findActivePolicyByType(CouponPolicyType.WELCOME)
                .orElseThrow(() -> {
                    log.error("활성화된 웰컴 쿠폰 정책이 없습니다.");
                    return new CouponPolicyNotFoundException();
                });

        Coupon welcomeCoupon = couponRepository.findByCouponPolicy_CouponPolicyId(welcomePolicy.getCouponPolicyId())
                .orElseThrow(() -> {
                    log.error("웰컴 쿠폰이 존재하지 않습니다. policyId={}", welcomePolicy.getCouponPolicyId());
                    return new CouponNotFoundException();
                });

        try {
            issueMemberCoupon(userId, welcomeCoupon.getCouponId());
            log.info("웰컴 쿠폰 지급 성공. userId={}", userId);
        } catch (Exception e) {
            log.error("웰컴 쿠폰 지급 중 예외 발생. userId={}, error={}", userId, e.getMessage());
            throw e;
        }

        log.info("웰컴 쿠폰 지급 성공 userId = {}", userId);
    }

    @Transactional
    @Override
    public void issueBirthdayCoupon(Long userId) {
        CouponPolicy birthdayPolicy = policyRepository.findActivePolicyByType(CouponPolicyType.BIRTHDAY)
                .orElseThrow(() -> {
                    log.warn("활성화된 생일 쿠폰 정책이 없습니다. userId={}", userId);
                    return new CouponPolicyNotFoundException();
                });

        Coupon birthdayCoupon = couponRepository.findByCouponPolicy_CouponPolicyId(birthdayPolicy.getCouponPolicyId())
                .orElseThrow(() -> {
                    log.error("생일 쿠폰이 존재하지 않습니다. policyId={}", birthdayPolicy.getCouponPolicyId());
                    return new CouponNotFoundException();
                });

        try {
            issueMemberCoupon(userId, birthdayCoupon.getCouponId());
            log.info("생일 쿠폰 지급 성공. userId={}", userId);
        } catch (Exception e) {
            log.error("생일 쿠폰 지급 중 예외 발생. userId={}, error={}", userId, e.getMessage());
            throw e;
        }
    }

    //적용가능한 쿠폰 확인 (쿠폰 다운로드용)
    @Transactional(readOnly = true)
    @Override
    public List<CouponResponseDto> getIssuableCoupons(Long userId, Long bookId, List<Long> categoryIds) {
        log.debug("상품 적용 가능 쿠폰 조회 요청. bookId={}, categoryIds={}", bookId, categoryIds);

        List<Coupon> coupons = couponRepository.findAppliableCoupons(bookId, categoryIds);

        Set<Long> myCouponIds;

        if (userId != null) {
            List<Long> ids = memberCouponRepository.findAllCouponIdsByUserId(userId);
            myCouponIds = new HashSet<>(ids);
        } else {
            myCouponIds = Collections.emptySet();
        }

        return coupons.stream()
                .filter(coupon -> {
                    CouponPolicy policy = coupon.getCouponPolicy();
                    boolean isStockAvailable =
                            coupon.getCouponRemainingQuantity() == null || coupon.getCouponRemainingQuantity() > 0;
                    boolean policyIssuable = policy.isIssuable();
                    return isStockAvailable && policyIssuable;
                })
                .map(coupon -> {
                    CouponResponseDto dto = new CouponResponseDto(coupon);

                    if (myCouponIds.contains(coupon.getCouponId())) {
                        dto.setIsIssued();
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }


    //만료일 계산
    private LocalDateTime calculateExpirationDate(CouponPolicy policy, LocalDateTime now) {
        if (policy.getFixedEndDate() != null) {
            return policy.getFixedEndDate().atTime(23, 59, 59, 999999000);
        }
        if (policy.getDurationDays() != null) {
            return now.plusDays(policy.getDurationDays());
        }
        log.error("쿠폰 정책에 만료일 기준 누락. policyId={}", policy.getCouponPolicyId());
        throw new IllegalStateException("쿠폰 정책에 만료일 기준이 없습니다. policyId=" + policy.getCouponPolicyId());
    }
}
