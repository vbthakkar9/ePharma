package com.pharmacy.saas.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ResourceNotFoundException extends BusinessException {

    private final String resourceName;
    private final Object resourceId;

    public ResourceNotFoundException(String resourceName, Object resourceId) {
        super(
            "RESOURCE_NOT_FOUND",
            String.format("%s not found with id: %s", resourceName, resourceId),
            HttpStatus.NOT_FOUND,
            resourceName, resourceId
        );
        this.resourceName = resourceName;
        this.resourceId = resourceId;
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(
            "RESOURCE_NOT_FOUND",
            String.format("%s not found with %s: %s", resourceName, fieldName, fieldValue),
            HttpStatus.NOT_FOUND,
            resourceName, fieldName, fieldValue
        );
        this.resourceName = resourceName;
        this.resourceId = fieldValue;
    }

    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message, HttpStatus.NOT_FOUND);
        this.resourceName = null;
        this.resourceId = null;
    }
}