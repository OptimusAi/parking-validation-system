package ca.optimusAI.pv.tenant.repository;

import ca.optimusAI.pv.tenant.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    /** Used by ADMIN / Hibernate filter disabled path. */
    Page<Tenant> findAllByIsDeletedFalse(Pageable pageable);

    /** Used by CLIENT_ADMIN to list tenants within their client. */
    Page<Tenant> findAllByClientIdAndIsDeletedFalse(UUID clientId, Pageable pageable);

    /** Public endpoint — branding fetch (no auth). */
    Optional<Tenant> findByIdAndIsDeletedFalse(UUID id);

    List<Tenant> findAllByClientIdAndIsDeletedFalse(UUID clientId);

    /** Used by CLIENT_ADMIN multi-tenant: list tenants by their assigned tenant IDs. */
    Page<Tenant> findAllByIdInAndIsDeletedFalse(List<UUID> ids, Pageable pageable);

    /** Used at login time to resolve which client owns a given tenant. */
    @Query("SELECT t.clientId FROM Tenant t WHERE t.id = :tenantId AND t.isDeleted = false")
    Optional<UUID> findClientIdByTenantId(@Param("tenantId") UUID tenantId);
}
