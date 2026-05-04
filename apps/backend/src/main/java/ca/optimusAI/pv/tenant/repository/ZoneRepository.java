package ca.optimusAI.pv.tenant.repository;

import ca.optimusAI.pv.tenant.entity.Zone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface ZoneRepository extends JpaRepository<Zone, UUID> {

    /** Hibernate filter active — returns only zones for the current tenant. */
    Page<Zone> findAllByIsDeletedFalse(Pageable pageable);

    /** Used by ADMIN to list zones under a specific tenant. */
    Page<Zone> findAllByTenantIdAndIsDeletedFalse(UUID tenantId, Pageable pageable);

    /** Count zones for a tenant — used for tenant list enrichment. */
    long countByTenantIdAndIsDeletedFalse(UUID tenantId);

    /**
     * Native query — checks validation_sessions table without introducing a
     * cross-package entity dependency at this phase.
     * Returns true if any ACTIVE or EXTENDED session exists for the zone.
     */
    /** Report worker — fetches zones by IDs without Hibernate filter constraint. */
    List<Zone> findAllByIdIn(Collection<UUID> ids);

    @Query(
        value = "SELECT COUNT(*) > 0 FROM validation_sessions " +
                "WHERE zone_id = :zoneId AND status IN ('ACTIVE', 'EXTENDED') " +
                "AND is_deleted = false",
        nativeQuery = true
    )
    boolean hasActiveSessions(@Param("zoneId") UUID zoneId);
}
