package ca.optimusAI.tms.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.nimbusds.jose.jwk.JWKSet;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.concurrent.TimeUnit;

@Component
public class JWKSCache {

    private final Cache<String, JWKSet> cache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    public JWKSet get(String url) {
        return cache.get(url, u -> {
            try {
                return JWKSet.load(new URL(u));
            } catch (Exception e) {
                throw new RuntimeException("Failed to fetch JWKS from " + u, e);
            }
        });
    }
}
