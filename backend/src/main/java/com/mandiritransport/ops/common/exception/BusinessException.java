package com.mandiritransport.ops.common.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final Object data;

    public BusinessException(HttpStatus status, String errorCode, String message) {
        this(status, errorCode, message, null);
    }

    public BusinessException(HttpStatus status, String errorCode, String message, Object data) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.data = data;
    }

    public static BusinessException badRequest(String errorCode, String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, errorCode, message);
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
    }

    public static BusinessException conflict(String errorCode, String message, Object data) {
        return new BusinessException(HttpStatus.CONFLICT, errorCode, message, data);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Object getData() {
        return data;
    }
}
