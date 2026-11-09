package com.pharmacy.saas.common.constants;

public final class AuditActions {

    private AuditActions() {}

    // Tenant
    public static final String TENANT_CREATE = "TENANT_CREATE";
    public static final String TENANT_UPDATE = "TENANT_UPDATE";
    public static final String TENANT_UPDATED = "TENANT_UPDATED";
    public static final String TENANT_DELETE = "TENANT_DELETE";
    public static final String TENANT_DELETED = "TENANT_DELETED";
    public static final String TENANT_STATUS_CHANGE = "TENANT_STATUS_CHANGE";
    public static final String TENANT_STATUS_CHANGED = "TENANT_STATUS_CHANGED";

    // User
    public static final String USER_CREATE = "USER_CREATE";
    public static final String USER_CREATED = "USER_CREATED";
    public static final String USER_UPDATE = "USER_UPDATE";
    public static final String USER_UPDATED = "USER_UPDATED";
    public static final String USER_DELETE = "USER_DELETE";
    public static final String USER_DELETED = "USER_DELETED";
    public static final String USER_STATUS_CHANGE = "USER_STATUS_CHANGE";
    public static final String USER_STATUS_CHANGED = "USER_STATUS_CHANGED";
    public static final String USER_ROLE_ASSIGN = "USER_ROLE_ASSIGN";
    public static final String USER_ROLES_CHANGED = "USER_ROLES_CHANGED";
    public static final String USER_ROLE_REVOKE = "USER_ROLE_REVOKE";
    public static final String USER_PASSWORD_CHANGE = "USER_PASSWORD_CHANGE";
    public static final String USER_PASSWORD_RESET_REQUEST = "USER_PASSWORD_RESET_REQUEST";
    public static final String USER_PASSWORD_RESET_COMPLETE = "USER_PASSWORD_RESET_COMPLETE";

    // Authentication
    public static final String LOGIN = "LOGIN";
    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String LOGIN_FAILED = "LOGIN_FAILED";
    public static final String LOGOUT = "LOGOUT";
    public static final String PASSWORD_CHANGED = "PASSWORD_CHANGED";
    public static final String PASSWORD_RESET_REQUESTED = "PASSWORD_RESET_REQUESTED";
    public static final String PASSWORD_RESET_COMPLETED = "PASSWORD_RESET_COMPLETED";
    public static final String SESSION_EXPIRED = "SESSION_EXPIRED";

    // Tenant lifecycle
    public static final String TENANT_CREATED = "TENANT_CREATED";

    // Role
    public static final String ROLE_CREATE = "ROLE_CREATE";
    public static final String ROLE_CREATED = "ROLE_CREATED";
    public static final String ROLE_UPDATE = "ROLE_UPDATE";
    public static final String ROLE_UPDATED = "ROLE_UPDATED";
    public static final String ROLE_DELETE = "ROLE_DELETE";
    public static final String ROLE_DELETED = "ROLE_DELETED";

    // Permission
    public static final String PERMISSION_CREATE = "PERMISSION_CREATE";
    public static final String PERMISSION_CREATED = "PERMISSION_CREATED";
    public static final String PERMISSION_UPDATE = "PERMISSION_UPDATE";
    public static final String PERMISSION_UPDATED = "PERMISSION_UPDATED";
    public static final String PERMISSION_DELETE = "PERMISSION_DELETE";
    public static final String PERMISSION_DELETED = "PERMISSION_DELETED";
}