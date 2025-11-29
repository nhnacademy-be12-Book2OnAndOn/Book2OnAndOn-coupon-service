package com.example.book2onandoncouponservice.exception;

import lombok.Getter;

@Getter
public class CouponServiceException extends RuntimeException {

    private final CouponErrorCode errorCode;

    public CouponServiceException(CouponErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}