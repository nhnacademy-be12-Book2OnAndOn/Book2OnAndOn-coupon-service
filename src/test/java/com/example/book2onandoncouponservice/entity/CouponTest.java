package com.example.book2onandoncouponservice.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.book2onandoncouponservice.exception.CouponErrorCode;
import com.example.book2onandoncouponservice.exception.CouponIssueException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CouponTest {

    @Test
    @DisplayName("생성자 테스트: 무제한 수량 생성")
    void constructor_Unlimited() {
        Coupon coupon = new Coupon(null, null);
        assertThat(coupon.getCouponRemainingQuantity()).isNull();
    }

    @Test
    @DisplayName("수량 업데이트: 정상 수량")
    void update_Quantity() {
        Coupon coupon = new Coupon(10, null);
        coupon.update(50);
        assertThat(coupon.getCouponRemainingQuantity()).isEqualTo(50);
    }

    @Test
    @DisplayName("수량 업데이트: 무제한(null)으로 변경")
    void update_Quantity_ToNull() {
        Coupon coupon = new Coupon(10, null);
        coupon.update(null);
        assertThat(coupon.getCouponRemainingQuantity()).isNull();
    }

    @Test
    @DisplayName("수량 업데이트: 음수 입력 시 예외 발생")
    void update_Quantity_Negative_Fail() {
        Coupon coupon = new Coupon(10, null);
        assertThatThrownBy(() -> coupon.update(-1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("음수일 수 없습니다");
    }

    @Test
    @DisplayName("재고 차감: 일반 차감 성공")
    void decreaseStock_Success() {
        Coupon coupon = new Coupon(10, null);
        coupon.decreaseStock();
        assertThat(coupon.getCouponRemainingQuantity()).isEqualTo(9);
    }

    @Test
    @DisplayName("재고 차감: 무제한(null)인 경우 차감되지 않음")
    void decreaseStock_Unlimited_NoChange() {
        Coupon coupon = new Coupon(null, null);
        coupon.decreaseStock();
        assertThat(coupon.getCouponRemainingQuantity()).isNull();
    }

    @Test
    @DisplayName("재고 차감: 재고가 0 이하일 때 예외 발생")
    void decreaseStock_Fail_OutOfStock() {
        Coupon coupon = new Coupon(0, null);
        assertThatThrownBy(coupon::decreaseStock)
                .isInstanceOf(CouponIssueException.class)
                .hasMessage(CouponErrorCode.COUPON_OUT_OF_STOCK.getMessage());
    }
}