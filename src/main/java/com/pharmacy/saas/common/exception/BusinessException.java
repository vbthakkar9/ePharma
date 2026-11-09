package com.pharmacy.saas.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;
    private final Object[] args;

    public BusinessException(String errorCode, String message, HttpStatus httpStatus, Object... args) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.args = args;
    }

    public BusinessException(String errorCode, String message, HttpStatus httpStatus, Throwable cause, Object... args) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.args = args;
    }

    // Convenience constructors with default HTTP status
    public BusinessException(String errorCode, String message) {
        this(errorCode, message, HttpStatus.BAD_REQUEST);
    }

    public BusinessException(String errorCode, String message, Throwable cause) {
        this(errorCode, message, HttpStatus.BAD_REQUEST, cause);
    }

    public static BusinessException of(String errorCode, String message, HttpStatus httpStatus, Object... args) {
        return new BusinessException(errorCode, message, httpStatus, args);
    }

    public static BusinessException conflict(String errorCode, String message, Object... args) {
        return new BusinessException(errorCode, message, HttpStatus.CONFLICT, args);
    }

    public static BusinessException notFound(String errorCode, String message, Object... args) {
        return new BusinessException(errorCode, message, HttpStatus.NOT_FOUND, args);
    }

    public static BusinessException badRequest(String errorCode, String message, Object... args) {
        return new BusinessException(errorCode, message, HttpStatus.BAD_REQUEST, args);
    }

    public static BusinessException forbidden(String errorCode, String message, Object... args) {
        return new BusinessException(errorCode, message, HttpStatus.FORBIDDEN, args);
    }

    public static BusinessException unauthorized(String errorCode, String message, Object... args) {
        return new BusinessException(errorCode, message, HttpStatus.UNAUTHORIZED, args);
    }

    public static BusinessException internal(String errorCode, String message, Object... args) {
        return new BusinessException(errorCode, message, HttpStatus.INTERNAL_SERVER_ERROR, args);
    }
}