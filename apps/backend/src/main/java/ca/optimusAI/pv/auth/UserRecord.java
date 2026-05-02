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
                null,  // tenantId — scope now in dedicated assignment tables
                null,  // clientId — scope now in dedicated assignment tables
                null,  // subTenantId — scope now in dedicated assignment tables
                userRole != null ? userRole.getRole() : "USER",
                user.isActive()
        );
    }
}
