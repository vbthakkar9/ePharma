package com.pharmacy.saas.security.handler;

import com.pharmacy.saas.common.constants.ApplicationConstants;
import com.pharmacy.saas.security.service.CustomUserDetails;
import com.pharmacy.saas.security.session.SessionManagementService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final SessionManagementService sessionManagementService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // Get the existing session (created during authentication) - don't create a new one!
        // The session was already created by Spring Session when the authentication filter ran.
        HttpSession session = request.getSession(false);
        if (session != null) {
            sessionManagementService.registerSession(userDetails.getUserId(), session.getId());
        }

        userDetails.updateLastLogin(Instant.now());

        // Don't write response body here - let Spring Session filter save the session cookie
        // Just set status and content type. The response body will be empty but session cookie will be set.
        // Client should call /auth/me to get user profile.
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");

        log.info("User {} logged in successfully, session: {}", userDetails.getUsername(), session != null ? session.getId() : "none");
    }
}