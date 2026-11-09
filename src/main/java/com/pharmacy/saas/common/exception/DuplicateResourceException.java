package com.pharmacy.saas.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class DuplicateResourceException extends BusinessException {

    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(
            "DUPLICATE_RESOURCE",
            String.format("%s already exists with %s: %s", resourceName, fieldName, fieldValue),
            HttpStatus.CONFLICT,
            resourceName, fieldName, fieldValue
        );
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public DuplicateResourceException(String message) {
        super("DUPLICATE_RESOURCE", message, HttpStatus.CONFLICT);
        this.resourceName = null;
        this.fieldName = null;
        this.fieldValue = null;
    }
}