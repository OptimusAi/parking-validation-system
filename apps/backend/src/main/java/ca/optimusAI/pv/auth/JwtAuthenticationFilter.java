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
 * Intercepts every request that carries a Bearer token.
 *
 * Flow (post token-switch):
 *   1. Extract "Authorization: Bearer <internalToken>" header.
 *   2. Validate the HMAC-SHA256 signature + expiry via InternalTokenService.
 *   3. Populate TenantContext directly from the token claims — no DB/Redis hit.
 *   4. Populate Spring SecurityContext (used by @PreAuthorize).
 *   5. Clear TenantContext in the finally block — always.
 *
 * The token is the TMS-internal JWT signed at login time by InternalTokenService,
 * NOT the raw OAuth server token. The token-switch happens in AuthController.login().
 *
 * Requests without a Bearer header are passed through unchanged.
 *
 * Public auth paths (login, refresh, token-exchange) are skipped entirely so that
 * callers can pass an OAuth RS256 Bearer token directly to those endpoints without
 * the filter trying to validate it as an HS256 internal token.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final List<String> SKIP_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/token-exchange"
    );

    private final InternalTokenService internalTokenService;

    /** Skip JWT validation for public auth endpoints. */
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


            // Validate TMS-internal JWT (HMAC-SHA256) and extract full tenant context
            InternalClaims claims = internalTokenService.verify(token);

            // Populate TenantContext directly from claims — no DB round-trip needed
            TenantContext.set(new TenantInfo(
                    claims.tenantId(),
                    claims.clientId(),
                    claims.subTenantId(),
                    claims.userId(),
                    claims.email(),
                    List.of(claims.role() != null ? claims.role() : "SUBTENANT_USER")
            ));

            // Populate Spring SecurityContext for @PreAuthorize
            var authorities = List.of(new SimpleGrantedAuthority(
                    "ROLE_" + (claims.role() != null ? claims.role() : "SUBTENANT_USER")));
            var authentication = new UsernamePasswordAuthenticationToken(
                    claims.userId(), null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            chain.doFilter(req, res);

        } catch (InvalidTokenException e) {
            SecurityContextHolder.clearContext();
            writeUnauthorized(res, e.getMessage());
        } finally {
            // Always clear — must not leak across virtual-thread reuse
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
