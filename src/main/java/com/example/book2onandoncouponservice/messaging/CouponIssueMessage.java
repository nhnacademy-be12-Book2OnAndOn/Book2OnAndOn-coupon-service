package com.example.book2onandoncouponservice.messaging;

public record CouponIssueMessage(
        Long userId,
        Long couponId
) {
}