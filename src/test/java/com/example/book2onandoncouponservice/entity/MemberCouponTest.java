package com.example.book2onandoncouponservice.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.book2onandoncouponservice.exception.CouponErrorCode;
import com.example.book2onandoncouponservice.exception.CouponUseException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MemberCouponTest {

    @Test
    @DisplayName("Builder 생성 테스트: 기본 상태는 NOT_USED 여야 함")
    void builder_DefaultStatus() {
        MemberCoupon mc = MemberCoupon.builder()
                .userId(1L)
                .coupon(new Coupon())
                .memberCouponIssuedDate(LocalDateTime.now())
                .memberCouponEndDate(LocalDateTime.now().plusDays(1))
                .build();

        assertThat(mc.getMemberCouponStatus()).isEqualTo(MemberCouponStatus.NOT_USED);
    }

    @Test
    @DisplayName("쿠폰 사용: 정상 사용")
    void use_Success() {
        // given
        LocalDateTime future = LocalDateTime.now().plusDays(1);
        MemberCoupon mc = MemberCoupon.builder()
                .memberCouponEndDate(future)
                .build();

        String orderNumber = "100L";

        // when
        mc.use(orderNumber);

        // then
        assertThat(mc.getMemberCouponStatus()).isEqualTo(MemberCouponStatus.USED);
        assertThat(mc.getOrderNumber()).isEqualTo(orderNumber);
        assertThat(mc.getMemberCouponUsedDate()).isNotNull();
    }

    @Test
    @DisplayName("쿠폰 사용 실패: 이미 사용된 쿠폰")
    void use_Fail_AlreadyUsed() {
        // given
        MemberCoupon mc = MemberCoupon.builder().memberCouponEndDate(LocalDateTime.now().plusDays(1)).build();
        mc.use("100L"); // First usage

        // when & then
        assertThatThrownBy(() -> mc.use("101L"))
                .isInstanceOf(CouponUseException.class)
                .hasMessage(CouponErrorCode.COUPON_ALREADY_USED.getMessage());
    }

    @Test
    @DisplayName("쿠폰 사용 실패: 상태가 EXPIRED 인 경우")
    void use_Fail_StatusExpired() {
        // given
        MemberCoupon mc = MemberCoupon.builder().memberCouponEndDate(LocalDateTime.now().plusDays(1)).build();
        // 상태 강제 변경 (Setter가 없으므로 Reflection 사용)
        ReflectionTestUtils.setField(mc, "memberCouponStatus", MemberCouponStatus.EXPIRED);

        // when & then
        assertThatThrownBy(() -> mc.use("100L"))
                .isInstanceOf(CouponUseException.class)
                .hasMessage(CouponErrorCode.COUPON_EXPIRED.getMessage());
    }

    @Test
    @DisplayName("쿠폰 사용 실패: 날짜가 만료된 경우")
    void use_Fail_DateExpired() {
        // given: 어제 날짜로 만료일 설정
        LocalDateTime past = LocalDateTime.now().minusDays(1);
        MemberCoupon mc = MemberCoupon.builder()
                .memberCouponEndDate(past)
                .build();

        // when & then
        assertThatThrownBy(() -> mc.use("100L"))
                .isInstanceOf(CouponUseException.class)
                .hasMessage(CouponErrorCode.COUPON_EXPIRED.getMessage());
    }

    @Test
    @DisplayName("사용 취소: 정상 취소")
    void cancelUsage_Success() {
        // given
        MemberCoupon mc = MemberCoupon.builder().memberCouponEndDate(LocalDateTime.now().plusDays(1)).build();
        mc.use("100L"); // USED 상태로 만듦

        // when
        mc.cancelUsage();

        // then
        assertThat(mc.getMemberCouponStatus()).isEqualTo(MemberCouponStatus.NOT_USED);
        assertThat(mc.getOrderNumber()).isNull();
        assertThat(mc.getMemberCouponUsedDate()).isNull();
    }

    @Test
    @DisplayName("사용 취소 실패: 사용되지 않은 쿠폰")
    void cancelUsage_Fail_NotUsed() {
        // given
        MemberCoupon mc = MemberCoupon.builder().build(); // NOT_USED 상태

        // when & then
        assertThatThrownBy(mc::cancelUsage)
                .isInstanceOf(CouponUseException.class)
                .hasMessage(CouponErrorCode.COUPON_NOT_USED.getMessage());
    }
}