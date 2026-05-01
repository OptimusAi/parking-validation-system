package ca.optimusAI.pv.auth;

import ca.optimusAI.pv.shared.TenantContext;
import ca.optimusAI.pv.shared.TenantInfo;
import ca.optimusAI.pv.shared.exception.InvalidTokenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/**
 * Validates the TMS HS256 Bearer token on every request.
 *
 * Flow:
 *   1. Extract "Authorization: Bearer <tmsToken>" header.
 *   2. Validate HMAC-SHA256 signature + expiry via TmsTokenService.
 *   3. Populate TenantContext from token claims — no DB/Redis round-trip.
 *   4. Populate Spring SecurityContext for @PreAuthorize.
 *   5. Clear TenantContext in finally block — always.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final List<String> SKIP_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/token-exchange"
    );

    private final TmsTokenService tmsTokenService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return SKIP_PATHS.stream().anyMatch(path::equals);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain)
            throws ServletException, IOException {

        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(req, res);
            return;
        }

        try {
            String token = header.substring(7);

            TmsTokenClaims claims = tmsTokenService.verify(token);

            TenantContext.set(new TenantInfo(
                    claims.tenantId(),
                    claims.clientId(),
                    claims.subTenantId(),
                    claims.userId(),
                    claims.email(),
                    List.of(claims.role() != null ? claims.role() : "USER"),
                    claims.assignedTenants() != null ? claims.assignedTenants() : List.of()
            ));

            var authorities = List.of(new SimpleGrantedAuthority(
                    "ROLE_" + (claims.role() != null ? claims.role() : "USER")));
            var authentication = new UsernamePasswordAuthenticationToken(
                    claims.userId(), null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            chain.doFilter(req, res);

        } catch (InvalidTokenException e) {
            SecurityContextHolder.clearContext();
            TenantContext.clear();
            writeUnauthorized(res, e.getMessage());
        } finally {
            TenantContext.clear();
        }
    }

    private void writeUnauthorized(HttpServletResponse res, String message) throws IOException {
        res.setStatus(HttpStatus.UNAUTHORIZED.value());
        res.setContentType("application/json");
        res.getWriter().write(
                "{\"code\":\"INVALID_TOKEN\",\"message\":\"%s\",\"details\":{},\"timestamp\":\"%s\"}"
                        .formatted(message.replace("\"", "'"), Instant.now()));
    }
}
