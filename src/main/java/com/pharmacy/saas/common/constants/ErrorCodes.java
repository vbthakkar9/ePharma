package com.pharmacy.saas.common.constants;

public final class ErrorCodes {

    private ErrorCodes() {}

    // Generic
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String CONFLICT = "CONFLICT";
    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String CONSTRAINT_VIOLATION = "CONSTRAINT_VIOLATION";
    public static final String DATA_INTEGRITY_VIOLATION = "DATA_INTEGRITY_VIOLATION";
    public static final String ENDPOINT_NOT_FOUND = "ENDPOINT_NOT_FOUND";

    // Authentication
    public static final String AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    public static final String ACCOUNT_DISABLED = "ACCOUNT_DISABLED";
    public static final String ACCOUNT_INACTIVE = "ACCOUNT_INACTIVE";
    public static final String SESSION_EXPIRED = "SESSION_EXPIRED";
    public static final String SESSION_INVALID = "SESSION_INVALID";
    public static final String CSRF_TOKEN_MISSING = "CSRF_TOKEN_MISSING";
    public static final String CSRF_TOKEN_INVALID = "CSRF_TOKEN_INVALID";
    public static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";

    // Tenant
    public static final String TENANT_NOT_FOUND = "TENANT_NOT_FOUND";
    public static final String TENANT_CODE_EXISTS = "TENANT_CODE_EXISTS";
    public static final String TENANT_EMAIL_EXISTS = "TENANT_EMAIL_EXISTS";
    public static final String TENANT_CANNOT_DELETE = "TENANT_CANNOT_DELETE";
    public static final String TENANT_STATUS_INVALID = "TENANT_STATUS_INVALID";

    // User
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String USER_EMAIL_EXISTS = "USER_EMAIL_EXISTS";
    public static final String USER_MOBILE_EXISTS = "USER_MOBILE_EXISTS";
    public static final String USER_CANNOT_DELETE_SELF = "USER_CANNOT_DELETE_SELF";
    public static final String USER_CANNOT_MODIFY_SUPER_ADMIN = "USER_CANNOT_MODIFY_SUPER_ADMIN";
    public static final String INVALID_PASSWORD = "INVALID_PASSWORD";
    public static final String PASSWORD_MISMATCH = "PASSWORD_MISMATCH";
    public static final String PASSWORD_TOO_WEAK = "PASSWORD_TOO_WEAK";
    public static final String PASSWORD_RECENTLY_USED = "PASSWORD_RECENTLY_USED";

    // Role & Permission
    public static final String ROLE_NOT_FOUND = "ROLE_NOT_FOUND";
    public static final String ROLE_NAME_EXISTS = "ROLE_NAME_EXISTS";
    public static final String ROLE_IS_SYSTEM = "ROLE_IS_SYSTEM";
    public static final String ROLE_IN_USE = "ROLE_IN_USE";
    public static final String PERMISSION_NOT_FOUND = "PERMISSION_NOT_FOUND";
    public static final String PERMISSION_NAME_EXISTS = "PERMISSION_NAME_EXISTS";

    // Password Reset
    public static final String RESET_TOKEN_INVALID = "RESET_TOKEN_INVALID";
    public static final String RESET_TOKEN_EXPIRED = "RESET_TOKEN_EXPIRED";
    public static final String RESET_TOKEN_USED = "RESET_TOKEN_USED";
    public static final String RESET_TOKEN_NOT_FOUND = "RESET_TOKEN_NOT_FOUND";
}