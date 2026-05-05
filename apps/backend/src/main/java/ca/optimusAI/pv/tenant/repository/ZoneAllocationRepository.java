package ca.optimusAI.pv.tenant.repository;

import ca.optimusAI.pv.tenant.entity.ZoneAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ZoneAllocationRepository extends JpaRepository<ZoneAllocation, UUID> {
    Optional<ZoneAllocation> findByTenantId(UUID tenantId);
}
