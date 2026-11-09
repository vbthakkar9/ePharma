# ADR 004: BCrypt Password Hashing

## Status
Accepted

## Context
Need to choose a password hashing algorithm for user authentication. Options:
1. **BCrypt** - Adaptive, widely adopted, built into Spring Security
2. **Argon2** - Modern, memory-hard, winner of Password Hashing Competition
3. **PBKDF2** - NIST standard, configurable iterations
4. **Scrypt** - Memory-hard, older than Argon2

## Decision
Use **BCrypt with cost factor 12** via Spring Security's `BCryptPasswordEncoder`.

## Rationale

| Factor | BCrypt | Argon2 | PBKDF2 | Scrypt |
|--------|--------|--------|--------|--------|
| **Spring Support** | Native | Requires BouncyCastle | Native | Requires library |
| **Maturity** | 20+ years | 2015 | 20+ years | 2009 |
| **Hardware Resistance** | CPU-hard | Memory-hard | CPU-hard | Memory-hard |
| **Configurability** | Cost factor (4-31) | Time, memory, parallelism | Iterations | N, r, p |
| **Adoption** | Very high | Growing | High | Moderate |
| **JVM Performance** | Good | Slower (JNI) | Good | Moderate |

### Why BCrypt over Argon2?
- **Native Spring Support**: No additional dependencies
- **Proven Track Record**: 20+ years in production systems
- **Sufficient Security**: Cost 12 = ~250ms on modern hardware
- **Operational Simplicity**: No native library dependencies

## Configuration

```java
// PasswordEncoderConfig.java
@Value("${app.security.bcrypt-cost:12}")
private int bcryptCost;

@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(bcryptCost, new SecureRandom());
}
```

### Cost Factor Selection

| Cost | Hash Time (approx) | Use Case |
|------|-------------------|----------|
| 10 | ~50ms | High throughput, lower security |
| **12** | **~250ms** | **Recommended default** |
| 14 | ~1000ms | High security, lower throughput |

Cost 12 provides good balance:
- 250ms prevents brute force (4 hashes/second)
- Acceptable for login UX
- Configurable for future hardware improvements

## Password Policy

```java
// PasswordUtils.java
public static ValidationResult validatePassword(String password) {
    // Min 8, max 128 characters
    // Requires: uppercase, lowercase, digit, special char
    // No common password checks (can add later)
}
```

## Password Reset Tokens

Tokens are **not stored in plaintext**:
```java
// Generate cryptographically secure token
String rawToken = TokenGenerator.generateResetToken(); // 32 bytes base64

// Store SHA-256 hash
String tokenHash = PasswordUtils.hashToken(rawToken); // SHA-256

// Verify
boolean valid = PasswordUtils.hashToken(inputToken).equals(storedHash);
```

### Token Properties
- **Entropy**: 256 bits (32 bytes)
- **Expiry**: 15 minutes
- **Single-use**: Marked used after reset
- **Hashed storage**: SHA-256 (fast, deterministic)

## Migration Path

If upgrading to Argon2 in future:
```java
// Spring Security supports delegating encoder
@Bean
public PasswordEncoder passwordEncoder() {
    String idForEncode = "bcrypt";
    Map<String, PasswordEncoder> encoders = Map.of(
        "bcrypt", new BCryptPasswordEncoder(12),
        "argon2", new Argon2PasswordEncoder()
    );
    return new DelegatingPasswordEncoder(idForEncode, encoders);
}
```

Existing BCrypt hashes continue working; new hashes use Argon2.

## Security Considerations

1. **Timing Attacks**: BCryptPasswordEncoder uses constant-time comparison
2. **Rainbow Tables**: Salt is embedded in hash (`$2a$12$salt...`)
3. **DoS**: Cost factor limits hash rate; rate limiting on auth endpoints
4. **Credential Stuffing**: Rate limiting + account lockout

## Testing

```java
@Test
void bcryptCost12_shouldTakeReasonableTime() {
    PasswordEncoder encoder = new BCryptPasswordEncoder(12);
    long start = System.currentTimeMillis();
    encoder.encode("Test@123");
    long elapsed = System.currentTimeMillis() - start;
    
    // Should be between 100-500ms on CI hardware
    assertThat(elapsed).isBetween(100L, 500L);
}
```

## References
- [BCrypt Paper](https://www.usenix.org/legacy/events/usenix99/provos/provos_html/)
- [Spring Security Password Encoding](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/index.html)
- [OWASP Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
- [NIST SP 800-63B](https://pages.nist.gov/800-63-3/sp800-63b.html)