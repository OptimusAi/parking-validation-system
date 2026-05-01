package ca.optimusAI.pv.tenant.repository;

import ca.optimusAI.pv.tenant.entity.ClientAdminTenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientAdminTenantRepository extends JpaRepository<ClientAdminTenant, UUID> {

    List<ClientAdminTenant> findAllByUserId(UUID userId);

    @Query("SELECT c.tenantId FROM ClientAdminTenant c WHERE c.userId = :userId")
    List<UUID> findTenantIdsByUserId(@Param("userId") UUID userId);

    Optional<ClientAdminTenant> findByUserIdAndTenantId(UUID userId, UUID tenantId);

    void deleteByUserIdAndTenantId(UUID userId, UUID tenantId);
}
