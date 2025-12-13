package com.example.book2onandoncouponservice.dto.response;

import com.example.book2onandoncouponservice.entity.CouponPolicyDiscountType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CouponTargetResponseDto {
    private Long memberCouponId;
    private List<Long> targetBookIds;     // 적용 가능한 책 ID 목록
    private List<Long> targetCategoryIds; // 적용 가능한 카테고리 ID 목록
    private Integer minPrice; //최소 주문 금액
    private Integer maxPrice; //최대 할인 금액
    private CouponPolicyDiscountType discountType; //할인 유형 FIXED, PERCENT
    private Integer discountValue; //할인 값 FIXED일 시 (원) / PERCENT일 시 (%) 고려해서 계산 로직 작성
}
