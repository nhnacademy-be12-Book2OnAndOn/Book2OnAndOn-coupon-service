package com.example.book2onandoncouponservice.exception;

public class CouponUseException extends CouponServiceException {
    public CouponUseException(CouponErrorCode errorCode) {
        super(errorCode);
    }
}