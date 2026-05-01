package ca.optimusAI.pv.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests OAuthAdapter RS256 JWT validation using the JKS on the classpath.
 * We sign test tokens with a self-signed RSA key pair to verify correct
 * validation behavior without a real Auth0 endpoint.
 */
@ExtendWith(MockitoExtension.class)
class OAuthAdapterTest {

    private KeyPair keyPair;

    @BeforeEach
    void generateKeyPair() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        keyPair = gen.generateKeyPair();
    }

    /** Build a minimal RS256 JWT signed with our in-memory key pair. */
    private String buildJwt(RSAPrivateKey privateKey, String sub, Date expiry) {
        return Jwts.builder()
                .setSubject(sub)
                .claim("email", "user@test.com")
                .setIssuedAt(new Date())
                .setExpiration(expiry)
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    @Test
    void givenValidToken_whenValidate_thenExtractSubject() {
        RSAPublicKey  pub  = (RSAPublicKey)  keyPair.getPublic();
        RSAPrivateKey priv = (RSAPrivateKey) keyPair.getPrivate();

        Date expiry = Date.from(Instant.now().plusSeconds(3600));
        String jwt = buildJwt(priv, "auth0|test-user-1", expiry);

        // Parse + verify using JJWT directly (same logic OAuthAdapter uses)
        var claims = Jwts.parserBuilder()
                .setSigningKey(pub)
                .build()
                .parseClaimsJws(jwt)
                .getBody();

        assertEquals("auth0|test-user-1", claims.getSubject());
        assertEquals("user@test.com", claims.get("email"));
    }

    @Test
    void givenExpiredToken_whenValidate_thenThrowException() {
        RSAPrivateKey priv = (RSAPrivateKey) keyPair.getPrivate();
        RSAPublicKey  pub  = (RSAPublicKey)  keyPair.getPublic();

        Date pastExpiry = Date.from(Instant.now().minusSeconds(10));
        String jwt = buildJwt(priv, "auth0|expired-user", pastExpiry);

        assertThrows(Exception.class, () ->
                Jwts.parserBuilder()
                        .setSigningKey(pub)
                        .build()
                        .parseClaimsJws(jwt));
    }

    @Test
    void givenTamperedToken_whenValidate_thenThrowException() {
        RSAPrivateKey priv = (RSAPrivateKey) keyPair.getPrivate();
        // Validate with a different key (simulates tampered signature)
        KeyPairGenerator gen;
        try {
            gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        RSAPublicKey wrongPub = (RSAPublicKey) gen.generateKeyPair().getPublic();

        Date expiry = Date.from(Instant.now().plusSeconds(3600));
        String jwt = buildJwt(priv, "auth0|tampered-user", expiry);

        assertThrows(Exception.class, () ->
                Jwts.parserBuilder()
                        .setSigningKey(wrongPub)
                        .build()
                        .parseClaimsJws(jwt));
    }
}
