package com.example.book2onandoncouponservice.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.book2onandoncouponservice.dto.error.ErrorResponse;
import com.example.book2onandoncouponservice.exception.CouponIssueException;
import com.example.book2onandoncouponservice.exception.CouponNotFoundException;
import com.example.book2onandoncouponservice.exception.CouponPolicyNotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("handleNotFound: 404 Not Found 예외 처리 (CouponNotFoundException)")
    void handleNotFound_CouponNotFoundException() {
        // given
        CouponNotFoundException ex = mock(CouponNotFoundException.class);
        when(ex.getMessage()).thenReturn("Coupon not found");

        // when
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleNotFound(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Coupon not found");
        assertThat(response.getBody().getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("handleNotFound: 404 Not Found 예외 처리 (CouponPolicyNotFoundException)")
    void handleNotFound_CouponPolicyNotFoundException() {
        // given
        CouponPolicyNotFoundException ex = mock(CouponPolicyNotFoundException.class);
        when(ex.getMessage()).thenReturn("Policy not found");

        // when
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleNotFound(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        Assertions.assertNotNull(response.getBody());
        assertThat(response.getBody().getMessage()).isEqualTo("Policy not found");
    }

    @Test
    @DisplayName("handleBadRequest: 400 Bad Request 예외 처리 (CouponIssueException)")
    void handleBadRequest_CouponIssueException() {
        // given
        CouponIssueException ex = mock(CouponIssueException.class);
        when(ex.getMessage()).thenReturn("Issue failed");

        // when
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBadRequest(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertNotNull(response.getBody());
        assertThat(response.getBody().getMessage()).isEqualTo("Issue failed");
        assertThat(response.getBody().getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("handleBadRequest: 400 Bad Request 예외 처리 (MethodArgumentNotValidException)")
    void handleBadRequest_MethodArgumentNotValidException() {
        // given
        RuntimeException ex = mock(RuntimeException.class);
        when(ex.getMessage()).thenReturn("Validation failed");

        // when
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBadRequest(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        // MethodArgumentNotValidException은 getMessage() 호출 시 상세 정보가 리턴됨
        Assertions.assertNotNull(response.getBody());
        assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
    }

    @Test
    @DisplayName("handleConflict: 409 Conflict 예외 처리")
    void handleConflict() {
        // given
        IllegalStateException ex = new IllegalStateException("Conflict occurred");

        // when
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleConflict(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        Assertions.assertNotNull(response.getBody());
        assertThat(response.getBody().getMessage()).isEqualTo("Conflict occurred");
        assertThat(response.getBody().getStatus()).isEqualTo(409);
    }

    @Test
    @DisplayName("handleException: 500 Internal Server Error 처리")
    void handleException() {
        // given
        Exception ex = new Exception("Unexpected error");

        // when
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleException(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        Assertions.assertNotNull(response.getBody());
        assertThat(response.getBody().getMessage()).isEqualTo("서버 내부 오류가 발생했습니다.");
        assertThat(response.getBody().getStatus()).isEqualTo(500);
    }
}