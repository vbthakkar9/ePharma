package com.pharmacy.saas.common.constants;

public final class ApplicationConstants {

    private ApplicationConstants() {}

    public static final String TENANT_ID_HEADER = "X-Tenant-ID";
    public static final String TENANT_ID_ATTRIBUTE = "tenantId";
    public static final String CURRENT_USER_ATTRIBUTE = "currentUser";
    public static final String SESSION_COOKIE_NAME = "SESSION";

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final String DEFAULT_SORT_DIRECTION = "ASC";
    public static final String DEFAULT_SORT_PROPERTY = "createdAt";

    public static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";
    public static final String PHARMACY_ADMIN_ROLE = "PHARMACY_ADMIN";
    public static final String PHARMACIST_ROLE = "PHARMACIST";
    public static final String PHARMACY_STAFF_ROLE = "PHARMACY_STAFF";

    public static final String TENANT_ACTIVE = "ACTIVE";
    public static final String TENANT_SUSPENDED = "SUSPENDED";
    public static final String TENANT_CLOSED = "CLOSED";

    public static final String USER_ACTIVE = "ACTIVE";
    public static final String USER_INACTIVE = "INACTIVE";
    public static final String USER_LOCKED = "LOCKED";
    public static final String USER_PENDING_VERIFICATION = "PENDING_VERIFICATION";

    public static final int MAX_LOGIN_ATTEMPTS = 5;
    public static final int LOGIN_LOCKOUT_DURATION_MINUTES = 15;
    public static final int MAX_CONCURRENT_SESSIONS = 5;
    public static final int SESSION_IDLE_TIMEOUT_MINUTES = 30;
    public static final int SESSION_MAX_AGE_HOURS = 8;
    public static final int PASSWORD_RESET_TOKEN_EXPIRY_MINUTES = 15;

    public static final String AUDIT_ACTION_CREATE = "CREATE";
    public static final String AUDIT_ACTION_UPDATE = "UPDATE";
    public static final String AUDIT_ACTION_DELETE = "DELETE";
    public static final String AUDIT_ACTION_LOGIN = "LOGIN";
    public static final String AUDIT_ACTION_LOGOUT = "LOGOUT";
    public static final String AUDIT_ACTION_FAILED_LOGIN = "FAILED_LOGIN";
    public static final String AUDIT_ACTION_PASSWORD_CHANGE = "PASSWORD_CHANGE";
    public static final String AUDIT_ACTION_PASSWORD_RESET = "PASSWORD_RESET";
    public static final String AUDIT_ACTION_ROLE_ASSIGN = "ROLE_ASSIGN";
    public static final String AUDIT_ACTION_ROLE_REVOKE = "ROLE_REVOKE";
    public static final String AUDIT_ACTION_STATUS_CHANGE = "STATUS_CHANGE";
}