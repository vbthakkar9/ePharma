package com.pharmacy.saas.security.config;

import com.pharmacy.saas.common.constants.ApplicationConstants;
import com.pharmacy.saas.security.filter.TenantContextFilter;
import com.pharmacy.saas.security.handler.CustomAuthenticationFailureHandler;
import com.pharmacy.saas.security.handler.CustomAuthenticationSuccessHandler;
import com.pharmacy.saas.security.handler.CustomLogoutSuccessHandler;
import com.pharmacy.saas.security.service.CustomUserDetailsService;
import com.pharmacy.saas.security.service.RateLimitingService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final CustomAuthenticationSuccessHandler authenticationSuccessHandler;
    private final CustomAuthenticationFailureHandler authenticationFailureHandler;
    private final CustomLogoutSuccessHandler logoutSuccessHandler;
    private final TenantContextFilter tenantContextFilter;
    private final RateLimitingService rateLimitingService;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .httpBasic(AbstractHttpConfigurer::disable)

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(5)
                        .maxSessionsPreventsLogin(false)
                        .expiredUrl("/auth/login?expired")
                )

                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers(
                                new AntPathRequestMatcher("/auth/login"),
                                new AntPathRequestMatcher("/auth/forgot-password"),
                                new AntPathRequestMatcher("/auth/reset-password"),
                                new AntPathRequestMatcher("/auth/csrf"),
                                new AntPathRequestMatcher("/auth/logout"),
                                new AntPathRequestMatcher("/actuator/**"),
                                new AntPathRequestMatcher("/v3/api-docs/**"),
                                new AntPathRequestMatcher("/swagger-ui/**"),
                                new AntPathRequestMatcher("/swagger-ui.html")
                        )
                )

                .cors(cors -> cors.configurationSource(request -> {
                    var corsConfig = new org.springframework.web.cors.CorsConfiguration();
                    corsConfig.setAllowedOrigins(java.util.List.of("http://localhost:3000", "http://localhost:5173"));
                    corsConfig.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                    corsConfig.setAllowedHeaders(java.util.List.of("*"));
                    corsConfig.setAllowCredentials(true);
                    corsConfig.setMaxAge(3600L);
                    return corsConfig;
                }))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/login",
                                "/auth/forgot-password",
                                "/auth/reset-password",
                                "/auth/csrf",
                                "/actuator/health",
                                "/actuator/info",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers("/auth/**").authenticated()
                        .requestMatchers("/tenants/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/users/**").hasAnyRole("SUPER_ADMIN", "PHARMACY_ADMIN")
                        .requestMatchers("/roles/**").hasAnyRole("SUPER_ADMIN", "PHARMACY_ADMIN")
                        .requestMatchers("/permissions/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/audit-logs/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/dashboard/**").authenticated()
                        .anyRequest().authenticated()
                )

                .authenticationManager(authenticationManager())

                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/auth/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(authenticationSuccessHandler)
                        .failureHandler(authenticationFailureHandler)
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessHandler(logoutSuccessHandler)
                        .invalidateHttpSession(true)
                        .deleteCookies(ApplicationConstants.SESSION_COOKIE_NAME, "XSRF-TOKEN")
                        .permitAll()
                )

                .addFilterBefore(tenantContextFilter, UsernamePasswordAuthenticationFilter.class)

                .exceptionHandling(ex -> ex
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.getWriter().write("""
                                    {"success":false,"errorCode":"ACCESS_DENIED","message":"Access denied","timestamp":"%s"}
                                    """.formatted(java.time.Instant.now()));
                        })
                );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        provider.setHideUserNotFoundExceptions(false);
        return new ProviderManager(provider);
    }
}