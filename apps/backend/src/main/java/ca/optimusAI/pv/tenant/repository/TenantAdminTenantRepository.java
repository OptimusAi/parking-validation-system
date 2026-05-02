package ca.optimusAI.pv.tenant.repository;

import ca.optimusAI.pv.tenant.entity.TenantAdminTenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantAdminTenantRepository extends JpaRepository<TenantAdminTenant, UUID> {

    Optional<TenantAdminTenant> findByUserId(UUID userId);

    @Query("SELECT t.tenantId FROM TenantAdminTenant t WHERE t.userId = :userId")
    Optional<UUID> findTenantIdByUserId(@Param("userId") UUID userId);

    void deleteByUserId(UUID userId);
}
