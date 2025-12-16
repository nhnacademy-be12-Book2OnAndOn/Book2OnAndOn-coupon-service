package com.example.book2onandoncouponservice.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.example.book2onandoncouponservice.dto.request.CouponPolicyRequestDto;
import com.example.book2onandoncouponservice.dto.request.CouponPolicyUpdateRequestDto;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CouponPolicyTest {

    @Test
    @DisplayName("생성자 테스트: DTO로부터 엔티티가 올바르게 생성되는지 확인")
    void constructor_FromDto_Success() {
        // given
        CouponPolicyRequestDto req = mock(CouponPolicyRequestDto.class);
        given(req.getCouponPolicyName()).willReturn("Test Policy");
        given(req.getCouponPolicyType()).willReturn(CouponPolicyType.WELCOME);
        given(req.getCouponPolicyDiscountType()).willReturn(CouponPolicyDiscountType.FIXED);
        given(req.getCouponDiscountValue()).willReturn(1000);
        given(req.getMinPrice()).willReturn(5000);
        given(req.getMaxPrice()).willReturn(2000);
        given(req.getDurationDays()).willReturn(30);
        given(req.getFixedStartDate()).willReturn(null);
        given(req.getFixedEndDate()).willReturn(null);

        // when
        CouponPolicy policy = new CouponPolicy(req);

        // then
        assertThat(policy.getCouponPolicyName()).isEqualTo("Test Policy");
        assertThat(policy.getCouponPolicyType()).isEqualTo(CouponPolicyType.WELCOME);
        assertThat(policy.getCouponPolicyStatus()).isEqualTo(CouponPolicyStatus.ACTIVE);
    }

    @Test
    @DisplayName("수정 테스트: 일반 필드 업데이트 (값이 있는 경우)")
    void updatePolicy_BasicFields() {
        // given
        CouponPolicy policy = CouponPolicy.builder()
                .couponPolicyName("Old Name")
                .minPrice(1000)
                .build();

        CouponPolicyUpdateRequestDto req = mock(CouponPolicyUpdateRequestDto.class);
        given(req.getCouponPolicyName()).willReturn("New Name");
        given(req.getMinPrice()).willReturn(2000);
        // 나머지는 null 반환 (기본 mock 동작)

        // when
        policy.updatePolicy(req);

        // then
        assertThat(policy.getCouponPolicyName()).isEqualTo("New Name");
        assertThat(policy.getMinPrice()).isEqualTo(2000);
    }

    @Test
    @DisplayName("수정 테스트: 일반 필드 null 입력 시 업데이트 안 함")
    void updatePolicy_BasicFields_Null_NoChange() {
        // given
        CouponPolicy policy = CouponPolicy.builder()
                .couponPolicyName("Original")
                .build();

        CouponPolicyUpdateRequestDto req = mock(CouponPolicyUpdateRequestDto.class);
        given(req.getCouponPolicyName()).willReturn(null);

        // when
        policy.updatePolicy(req);

        // then
        assertThat(policy.getCouponPolicyName()).isEqualTo("Original");
    }

    @Test
    @DisplayName("수정 테스트: MaxPrice 삭제 플래그 True")
    void updatePolicy_RemoveMaxPrice() {
        // given
        CouponPolicy policy = CouponPolicy.builder().maxPrice(5000).build();
        CouponPolicyUpdateRequestDto req = mock(CouponPolicyUpdateRequestDto.class);
        given(req.getRemoveMaxPrice()).willReturn(true);

        // when
        policy.updatePolicy(req);

        // then
        assertThat(policy.getMaxPrice()).isNull();
    }

    @Test
    @DisplayName("수정 테스트: MaxPrice 삭제 플래그 False이고 값 존재 시 업데이트")
    void updatePolicy_UpdateMaxPrice() {
        // given
        CouponPolicy policy = CouponPolicy.builder().maxPrice(5000).build();
        CouponPolicyUpdateRequestDto req = mock(CouponPolicyUpdateRequestDto.class);
        given(req.getRemoveMaxPrice()).willReturn(false); // or null
        given(req.getMaxPrice()).willReturn(10000);

        // when
        policy.updatePolicy(req);

        // then
        assertThat(policy.getMaxPrice()).isEqualTo(10000);
    }

    @Test
    @DisplayName("수정 테스트: Duration 삭제 플래그 True")
    void updatePolicy_RemoveDuration() {
        // given
        CouponPolicy policy = CouponPolicy.builder().durationDays(30).build();
        CouponPolicyUpdateRequestDto req = mock(CouponPolicyUpdateRequestDto.class);
        given(req.getRemoveDurationDays()).willReturn(true);

        // when
        policy.updatePolicy(req);

        // then
        assertThat(policy.getDurationDays()).isNull();
    }

    @Test
    @DisplayName("수정 테스트: Duration 삭제 플래그 False이고 값 존재 시 업데이트")
    void updatePolicy_UpdateDuration() {
        // given
        CouponPolicy policy = CouponPolicy.builder().durationDays(30).build();
        CouponPolicyUpdateRequestDto req = mock(CouponPolicyUpdateRequestDto.class);
        given(req.getRemoveDurationDays()).willReturn(false);
        given(req.getDurationDays()).willReturn(60);

        // when
        policy.updatePolicy(req);

        // then
        assertThat(policy.getDurationDays()).isEqualTo(60);
    }

    @Test
    @DisplayName("수정 테스트: FixedDate 삭제 플래그 True")
    void updatePolicy_RemoveFixedDate() {
        // given
        CouponPolicy policy = CouponPolicy.builder()
                .fixedStartDate(LocalDate.now())
                .fixedEndDate(LocalDate.now().plusDays(1))
                .build();

        CouponPolicyUpdateRequestDto req = mock(CouponPolicyUpdateRequestDto.class);
        given(req.getRemoveFixedDate()).willReturn(true);

        // when
        policy.updatePolicy(req);

        // then
        assertThat(policy.getFixedStartDate()).isNull();
        assertThat(policy.getFixedEndDate()).isNull();
    }

    @Test
    @DisplayName("수정 테스트: FixedDate 삭제 플래그 False이고 값 존재 시 업데이트")
    void updatePolicy_UpdateFixedDate() {
        // given
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        CouponPolicy policy = CouponPolicy.builder().build();
        CouponPolicyUpdateRequestDto req = mock(CouponPolicyUpdateRequestDto.class);
        given(req.getRemoveFixedDate()).willReturn(false);
        given(req.getFixedStartDate()).willReturn(today);
        given(req.getFixedEndDate()).willReturn(tomorrow);

        // when
        policy.updatePolicy(req);

        // then
        assertThat(policy.getFixedStartDate()).isEqualTo(today);
        assertThat(policy.getFixedEndDate()).isEqualTo(tomorrow);
    }

    @Test
    @DisplayName("상태 변경: 비활성화 및 발급 가능 여부 확인")
    void statusCheck() {
        // given
        CouponPolicy policy = CouponPolicy.builder()
                .couponPolicyStatus(CouponPolicyStatus.ACTIVE)
                .build();

        // when & then
        assertThat(policy.isIssuable()).isTrue();

        policy.deActive();
        assertThat(policy.getCouponPolicyStatus()).isEqualTo(CouponPolicyStatus.DEACTIVE);
        assertThat(policy.isIssuable()).isFalse();
    }
}