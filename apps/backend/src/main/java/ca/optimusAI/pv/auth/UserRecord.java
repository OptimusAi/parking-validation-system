package ca.optimusAI.pv.auth;

import ca.optimusAI.pv.user.entity.AppUser;

import java.util.UUID;

public record UserRecord(
        UUID id,
        String authProviderUserId,
        String email,
        String name,
        UUID tenantId,
        UUID clientId,
        UUID subTenantId,
        String role,
        boolean isActive
) {
    public static UserRecord from(AppUser user) {
        return new UserRecord(
                user.getId(),
                user.getAuthProviderUserId(),
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
