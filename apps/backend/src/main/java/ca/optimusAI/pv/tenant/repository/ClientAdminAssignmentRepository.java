package ca.optimusAI.pv.tenant.repository;

import ca.optimusAI.pv.tenant.entity.ClientAdminAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientAdminAssignmentRepository extends JpaRepository<ClientAdminAssignment, UUID> {

    List<ClientAdminAssignment> findAllByUserId(UUID userId);

    List<ClientAdminAssignment> findAllByUserIdIn(Collection<UUID> userIds);

    @Query("SELECT a.tenantId FROM ClientAdminAssignment a WHERE a.userId = :userId")
    List<UUID> findTenantIdsByUserId(@Param("userId") UUID userId);

    @Query("SELECT DISTINCT a.clientId FROM ClientAdminAssignment a WHERE a.userId = :userId")
    Optional<UUID> findClientIdByUserId(@Param("userId") UUID userId);

    Optional<ClientAdminAssignment> findByUserIdAndTenantId(UUID userId, UUID tenantId);

    void deleteByUserIdAndTenantId(UUID userId, UUID tenantId);

    @Query("SELECT a.userId FROM ClientAdminAssignment a WHERE a.tenantId IN :tenantIds")
    List<UUID> findUserIdsByTenantIdIn(@Param("tenantIds") List<UUID> tenantIds);
}
