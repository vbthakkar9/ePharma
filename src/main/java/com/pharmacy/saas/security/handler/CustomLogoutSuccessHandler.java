package com.pharmacy.saas.security.handler;

import com.pharmacy.saas.common.constants.ApplicationConstants;
import com.pharmacy.saas.security.session.SessionManagementService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    private final SessionManagementService sessionManagementService;

    @Override
    public void onLogoutSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        HttpSession session = request.getSession(false);
        if (session != null) {
            sessionManagementService.unregisterSession(session.getId());
            session.invalidate();
        }

        clearCookie(response, ApplicationConstants.SESSION_COOKIE_NAME);
        clearCookie(response, "XSRF-TOKEN");

        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(String.format("""
                {
                  "success": true,
                  "message": "Logged out successfully",
                  "timestamp": "%s"
                }
                """, Instant.now()));

        log.info("User logged out successfully");
    }

    private void clearCookie(HttpServletResponse response, String cookieName) {
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie(cookieName, "");
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}