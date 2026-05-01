package ca.optimusAI.tms.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Full token response from the OAuth 2.0 password-grant endpoint
 * (http://localhost:9090/oauth/token).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OAuthTokenResponse(
        @JsonProperty("access_token")  String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("token_type")    String tokenType,
        @JsonProperty("expires_in")    long expiresIn,
        @JsonProperty("scope")         String scope,
        @JsonProperty("firstName")     String firstName,
        @JsonProperty("lastName")      String lastName,
        @JsonProperty("provider")      String provider,
        @JsonProperty("email")         String email,
        @JsonProperty("jti")           String jti
) {}

