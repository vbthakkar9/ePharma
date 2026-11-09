package com.pharmacy.saas.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import java.util.List;

@Getter
public class ValidationException extends BusinessException {

    private final List<ValidationError> errors;

    public ValidationException(String message, List<ValidationError> errors) {
        super("VALIDATION_FAILED", message, HttpStatus.BAD_REQUEST);
        this.errors = errors;
    }

    public ValidationException(List<ValidationError> errors) {
        this("Validation failed", errors);
    }

    @Getter
    public static class ValidationError {
        private final String field;
        private final String message;
        private final Object rejectedValue;

        public ValidationError(String field, String message, Object rejectedValue) {
            this.field = field;
            this.message = message;
            this.rejectedValue = rejectedValue;
        }

        public ValidationError(String field, String message) {
            this(field, message, null);
        }
    }
}