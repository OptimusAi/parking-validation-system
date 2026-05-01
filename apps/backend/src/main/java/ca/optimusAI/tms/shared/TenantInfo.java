package ca.optimusAI.tms.shared;

import java.util.List;
import java.util.UUID;

public record TenantInfo(
        UUID tenantId,
        UUID clientId,
        UUID subTenantId,
        String userId,
        String email,
        List<String> roles
) {}
