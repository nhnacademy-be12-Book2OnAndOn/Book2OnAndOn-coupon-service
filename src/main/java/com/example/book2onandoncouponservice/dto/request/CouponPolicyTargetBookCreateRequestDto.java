package com.example.book2onandoncouponservice.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CouponPolicyTargetBookCreateRequestDto {

    @NotNull(message = "정책 ID는 필수입니다.")
    private Long couponPolicyId;

    @NotEmpty(message = "추가할 도서 ID 리스트는 비어있을 수 없습니다.")
    private List<Long> bookIds;
}