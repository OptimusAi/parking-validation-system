package ca.optimusAI.pv.shared;

import java.util.List;
import java.util.UUID;

public record AuthUser(
        String userId,
        String email,
        String name,
        UUID tenantId,
        UUID clientId,
        UUID subTenantId,
        List<String> roles,
        String accessToken
) {}
