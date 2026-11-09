package com.pharmacy.saas.security.handler;

import com.pharmacy.saas.common.constants.ErrorCodes;
import com.pharmacy.saas.security.service.CustomUserDetailsService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final CustomUserDetailsService userDetailsService;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {

        String email = request.getParameter("email");
        String errorCode = ErrorCodes.INVALID_CREDENTIALS;
        String message = "Invalid email or password";
        int status = HttpServletResponse.SC_UNAUTHORIZED;

        if (exception instanceof BadCredentialsException) {
            errorCode = ErrorCodes.INVALID_CREDENTIALS;
            message = "Invalid email or password";
            incrementFailedAttempts(email);
        } else if (exception instanceof DisabledException) {
            errorCode = ErrorCodes.ACCOUNT_DISABLED;
            message = "Account is disabled";
            status = HttpServletResponse.SC_FORBIDDEN;
        } else if (exception instanceof LockedException) {
            errorCode = ErrorCodes.ACCOUNT_LOCKED;
            message = "Account is locked due to too many failed attempts. Try again later.";
            status = HttpServletResponse.SC_FORBIDDEN;
        } else if (exception instanceof AccountStatusException) {
            errorCode = ErrorCodes.ACCOUNT_DISABLED;
            message = exception.getMessage();
            status = HttpServletResponse.SC_FORBIDDEN;
        }

        log.warn("Login failed for email: {} - {}", email, exception.getMessage());

        response.setContentType("application/json");
        response.setStatus(status);
        response.getWriter().write(String.format("""
                {
                  "success": false,
                  "errorCode": "%s",
                  "message": "%s",
                  "timestamp": "%s"
                }
                """, errorCode, message, Instant.now()));
    }

    private void incrementFailedAttempts(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        try {
            userDetailsService.incrementFailedAttempts(email);
        } catch (Exception e) {
            log.error("Error incrementing failed attempts for {}: {}", email, e.getMessage());
        }
    }
}