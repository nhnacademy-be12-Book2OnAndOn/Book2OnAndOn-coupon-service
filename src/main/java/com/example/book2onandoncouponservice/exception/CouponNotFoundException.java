package com.example.book2onandoncouponservice.exception;

public class CouponNotFoundException extends CouponServiceException {
    public CouponNotFoundException() {
        super(CouponErrorCode.COUPON_NOT_FOUND);
    }
}