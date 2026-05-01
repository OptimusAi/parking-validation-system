package ca.optimusAI.pv.auth;

/**
 * Structured claims extracted from a validated Auth0 access token.
 * email and name may be null if the access token does not carry those claims.
 */
public record JwtClaims(String userId, String email, String name) {}
