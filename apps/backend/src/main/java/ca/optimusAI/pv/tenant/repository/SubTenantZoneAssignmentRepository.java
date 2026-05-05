package ca.optimusAI.pv.tenant.repository;

import ca.optimusAI.pv.tenant.entity.SubTenantZoneAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface SubTenantZoneAssignmentRepository extends JpaRepository<SubTenantZoneAssignment, UUID> {

    List<SubTenantZoneAssignment> findBySubTenantId(UUID subTenantId);

    void deleteBySubTenantIdAndZoneId(UUID subTenantId, UUID zoneId);

    void deleteBySubTenantId(UUID subTenantId);

    boolean existsBySubTenantIdAndZoneId(UUID subTenantId, UUID zoneId);

    /** All sub-tenant IDs that have been assigned a given zone. */
    List<SubTenantZoneAssignment> findByZoneId(UUID zoneId);
}
