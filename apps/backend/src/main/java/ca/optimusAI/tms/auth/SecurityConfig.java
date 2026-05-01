package ca.optimusAI.tms.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Spring Security configuration.
 *
 * Public endpoints (no JWT required):
 *   - /actuator/health, /actuator/info
 *   - /api/auth/**          (login, callback, refresh, logout)
 *   - /api/v1/validations/public/**  (QR scan — end users)
 *   - GET /api/v1/tenants/{id}/branding (QR page branding fetch — no auth)
 *   - /api/v1/links/by-token/**      (QR page link metadata fetch)
 *   - /ws/**                (WebSocket STOMP)
 *   - /swagger-ui/**, /v3/api-docs/**
 *
 * All other /api/** require a valid JWT processed by JwtAuthenticationFilter.
 * Fine-grained RBAC is handled by @PreAuthorize on individual service/controller methods.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Value("${app.base-url:http://localhost:3000}")
    private String appBaseUrl;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF disabled — stateless JWT API, no session cookies
            .csrf(AbstractHttpConfigurer::disable)

            // CORS — restricted to APP_BASE_URL only
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Stateless — no HTTP session
            .sessionManagement(s ->
                    s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                // Infrastructure
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                // Public auth endpoints — no token required
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                // Token exchange: caller has an OAuth RS256 token and wants a TMS-internal JWT
                .requestMatchers(HttpMethod.POST, "/api/auth/token-exchange").permitAll()
                // /api/auth/me and /api/auth/logout require a valid JWT

                // Public validation (end-user QR scan — zero friction)
                .requestMatchers("/api/v1/validations/public/**").permitAll()

                // Public branding fetch (QR page needs tenant colours without login)
                .requestMatchers(HttpMethod.GET, "/api/v1/tenants/*/branding").permitAll()

                // Public link metadata (QR page shows zone info without login)
                .requestMatchers("/api/v1/links/by-token/**").permitAll()

                // WebSocket STOMP upgrade
                .requestMatchers("/ws/**").permitAll()

                // API docs
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                                 "/v3/api-docs/**").permitAll()

                // Everything else requires a valid JWT
                .anyRequest().authenticated()
            )

            // Run our JWT filter before Spring's username/password filter
            .addFilterBefore(jwtAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class)

            // 401 for missing/invalid token, 403 for insufficient role
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, e) ->
                    writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "Authentication required"))
                .accessDeniedHandler((request, response, e) ->
                    writeError(response, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN", "Access denied"))
            );

        return http.build();
    }

    private void writeError(HttpServletResponse response, int status, String code, String message)
            throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of(
                "code", code,
                "message", message,
                "timestamp", Instant.now().toString()
        ));
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(appBaseUrl));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
