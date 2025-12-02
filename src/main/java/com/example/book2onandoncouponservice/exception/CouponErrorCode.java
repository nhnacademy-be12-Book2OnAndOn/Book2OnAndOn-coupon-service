package com.example.book2onandoncouponservice.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CouponErrorCode {

    // 404 Not Found
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 쿠폰입니다."),
    POLICY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 쿠폰 정책입니다."),
    MEMBER_COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자 쿠폰입니다."),

    // 400 Bad Request & 409 Conflict
    COUPON_ALREADY_ISSUED(HttpStatus.CONFLICT, "이미 발급받은 쿠폰입니다."),
    COUPON_OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "쿠폰이 모두 소진되었습니다."),
    COUPON_EXPIRED(HttpStatus.BAD_REQUEST, "만료된 쿠폰입니다."),
    COUPON_ALREADY_USED(HttpStatus.CONFLICT, "이미 사용된 쿠폰입니다."),
    COUPON_NOT_USED(HttpStatus.BAD_REQUEST, "사용 내역이 없는 쿠폰입니다."),
    POLICY_NOT_ISSUABLE(HttpStatus.BAD_REQUEST, "발급 가능한 상태의 정책이 아닙니다."),
    NOT_COUPON_OWNER(HttpStatus.FORBIDDEN, "해당 쿠폰의 소유자가 아닙니다."),
    INVALID_COUPON_ORDER_MATCH(HttpStatus.BAD_REQUEST, "해당 주문에 사용한 쿠폰이 아닙니다."),

    // 500 Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}