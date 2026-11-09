# ADR-001: Session-based Authentication over JWT

## Status
Accepted

## Context
We need to choose an authentication mechanism for the Pharmacy Management SaaS. The main options are:
1. **JWT (JSON Web Tokens)** - Stateless, token-based authentication
2. **Session-based** - Stateful, server-side session storage with cookies

## Decision
We will use **session-based authentication** with Spring Session JDBC backed by PostgreSQL.

## Rationale

### Security Advantages
| Aspect | Session-based | JWT |
|--------|---------------|-----|
| Token Revocation | Immediate (delete session) | Requires blocklist/short expiry |
| Token Theft Impact | Limited (HttpOnly cookie, SameSite=Strict) | Full access until expiry |
| CSRF Protection | Built-in (Spring Security CSRF) | Must implement manually |
| Secure Storage | HttpOnly cookie (inaccessible to JS) | localStorage/sessionStorage (XSS vulnerable) |
| Automatic Expiry | Sliding window (last access) | Fixed expiry (requires refresh tokens) |

### Operational Advantages
- **Simpler Mental Model**: No token refresh logic, no clock skew issues
- **Immediate Logout**: Delete session → user logged out everywhere
- **Audit Trail**: Natural session tracking for compliance
- **Concurrent Session Control**: Easy to limit active sessions per user
- **Spring Security Integration**: First-class support with `SessionAuthenticationStrategy`

### Compliance
- HIPAA/healthcare: Session invalidation on password change is immediate
- PCI-DSS: No sensitive tokens in browser storage
- Audit requirements: Session lifecycle naturally logged

## Implementation Details

### Session Configuration
```java
@EnableJdbcHttpSession(maxInactiveIntervalInSeconds = 28800) // 8 hours
public class SessionConfig {
    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSION");
        serializer.setHttpOnly(true);
        serializer.setSecure(true);        // HTTPS only
        serializer.setSameSite("Strict");  // CSRF protection
        serializer.setMaxAge(28800);
        return serializer;
    }
}
```

### Session Rotation
On successful authentication, generate new session ID:
```java
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                        HttpServletResponse response, 
                                        Authentication authentication) {
        // Rotate session ID to prevent fixation
        request.changeSessionId();
        // ... rest of logic
    }
}
```

### Concurrent Session Control
```java
.sessionManagement(session -> session
    .maximumSessions(5)
    .maxSessionsPreventsLogin(false)  // Invalidate oldest
    .expiredUrl("/api/auth/login?expired")
)
```

## Consequences

### Positive
- Stronger security posture by default
- Simpler implementation (less custom code)
- Better compliance alignment
- Natural audit trail

### Negative
- **Stateful**: Requires sticky sessions or shared session store (solved by Spring Session JDBC)
- **Scalability**: Session store becomes single point (mitigated by PostgreSQL + connection pooling)
- **Cross-domain**: Cookies don't work across domains (not needed for our SPA architecture)

## Alternatives Considered

### JWT with Refresh Tokens
Rejected due to:
- Complexity of token refresh logic
- Revocation requires distributed blocklist (Redis)
- Higher XSS risk (tokens in localStorage)
- Clock skew issues in distributed systems

### Hybrid (JWT for API, Sessions for Web)
Rejected due to:
- Two auth systems to maintain
- Inconsistent security model
- Complexity without clear benefit

## References
- [Spring Session Reference](https://docs.spring.io/spring-session/reference/)
- [OWASP Session Management Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html)
- [OWASP JWT Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_Cheat_Sheet.html)