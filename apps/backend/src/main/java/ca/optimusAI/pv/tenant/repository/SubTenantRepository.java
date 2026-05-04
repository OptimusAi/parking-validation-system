package ca.optimusAI.pv.tenant.repository;

import ca.optimusAI.pv.tenant.entity.SubTenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface SubTenantRepository extends JpaRepository<SubTenant, UUID> {

    /** Hibernate filter active — returns only sub-tenants for the current tenant. */
    Page<SubTenant> findAllByIsDeletedFalse(Pageable pageable);

    /** Used by ADMIN to list all sub-tenants under a specific tenant. */
    Page<SubTenant> findAllByTenantIdAndIsDeletedFalse(UUID tenantId, Pageable pageable);

    /** Count sub-tenants for a tenant — used for tenant list enrichment. */
    long countByTenantIdAndIsDeletedFalse(UUID tenantId);

    /** Report worker — fetches sub-tenants by IDs without Hibernate filter constraint. */
    List<SubTenant> findAllByIdIn(Collection<UUID> ids);
}
