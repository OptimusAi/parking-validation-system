package ca.optimusAI.pv.auth;

/**
 * Structured claims extracted from a validated OAuth access token.
 * firstName and lastName are sourced from the given_name / family_name OIDC claims.
 * Any field may be null if the token does not carry the corresponding claim.
 */
public record JwtClaims(String userId, String email, String firstName, String lastName) {}
