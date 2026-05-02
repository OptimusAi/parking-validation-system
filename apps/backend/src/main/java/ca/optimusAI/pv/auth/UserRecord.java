package ca.optimusAI.pv.auth;

import ca.optimusAI.pv.user.entity.AppUser;
import ca.optimusAI.pv.user.entity.UserRole;

import java.util.UUID;

public record UserRecord(
        UUID id,
        String authProviderUserId,
        String email,
        String firstName,
        String lastName,
        UUID tenantId,
        UUID clientId,
        UUID subTenantId,
        String role,
        boolean isActive
) {
    public static UserRecord from(AppUser user, UserRole userRole) {
        return new UserRecord(
                user.getId(),
                user.getAuthProviderUserId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                userRole != null ? userRole.getTenantId()    : null,
                userRole != null ? userRole.getClientId()    : null,
                userRole != null ? userRole.getSubTenantId() : null,
                userRole != null ? userRole.getRole()        : "USER",
                user.isActive()
        );
    }
}
