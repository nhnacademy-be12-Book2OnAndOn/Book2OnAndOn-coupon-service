package com.example.book2onandoncouponservice.exception;

public class CouponIssueException extends CouponServiceException {

    public CouponIssueException(CouponErrorCode errorCode) {
        super(errorCode);
    }
}