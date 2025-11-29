package com.example.book2onandoncouponservice.exception;

public class CouponPolicyNotFoundException extends CouponServiceException {
    public CouponPolicyNotFoundException() {
        super(CouponErrorCode.POLICY_NOT_FOUND);
    }
}