package ca.optimusAI.tms.tenant.repository;

import ca.optimusAI.tms.tenant.entity.QuotaConfig;
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
public interface QuotaConfigRepository extends JpaRepository<QuotaConfig, UUID> {

    /** Hibernate filter active — returns quota configs for the current tenant. */
    Page<QuotaConfig> findAllByIsDeletedFalse(Pageable pageable);

    /** Used by ADMIN to list quota configs for a specific tenant. */
    Page<QuotaConfig> findAllByTenantIdAndIsDeletedFalse(UUID tenantId, Pageable pageable);

    /** Used during quota enforcement in ValidationService. */
    @Query("SELECT q FROM QuotaConfig q WHERE q.tenantId = :tenantId AND q.period = :period " +
           "AND q.subTenantId IS NULL AND q.isDeleted = false")
    Optional<QuotaConfig> findTenantQuota(@Param("tenantId") UUID tenantId,
                                          @Param("period") String period);

    @Query("SELECT q FROM QuotaConfig q WHERE q.tenantId = :tenantId AND q.period = :period " +
           "AND q.subTenantId = :subTenantId AND q.isDeleted = false")
    Optional<QuotaConfig> findSubTenantQuota(@Param("tenantId") UUID tenantId,
                                             @Param("period") String period,
                                             @Param("subTenantId") UUID subTenantId);

    /** Returns all non-deleted quota configs for a tenant (used by ValidationService). */
    List<QuotaConfig> findAllByTenantIdAndIsDeletedFalse(UUID tenantId);
}
