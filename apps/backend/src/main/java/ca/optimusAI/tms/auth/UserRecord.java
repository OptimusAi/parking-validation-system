package ca.optimusAI.tms.auth;

import ca.optimusAI.tms.user.entity.User;

import java.util.UUID;

public record UserRecord(
        UUID id,
        String auth0UserId,
        String email,
        String name,
        UUID tenantId,
        UUID clientId,
        UUID subTenantId,
        String role,
        boolean isActive
) {
    public static UserRecord from(User user) {
        return new UserRecord(
                user.getId(),
                user.getAuth0UserId(),
                user.getEmail(),
                user.getName(),
                user.getTenantId(),
                user.getClientId(),
                user.getSubTenantId(),
                user.getRole(),
                user.isActive()
        );
    }
}
