package ca.optimusAI.tms.shared;

public record AuthTokens(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String tokenType
) {
    public AuthTokens(String accessToken, String refreshToken, long expiresIn) {
        this(accessToken, refreshToken, expiresIn, "Bearer");
    }
}
