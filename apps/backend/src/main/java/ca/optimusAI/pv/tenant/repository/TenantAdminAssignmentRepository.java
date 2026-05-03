package ca.optimusAI.pv.tenant.repository;

import ca.optimusAI.pv.tenant.entity.TenantAdminAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantAdminAssignmentRepository extends JpaRepository<TenantAdminAssignment, UUID> {

    Optional<TenantAdminAssignment> findByUserId(UUID userId);

    List<TenantAdminAssignment> findAllByUserIdIn(Collection<UUID> userIds);

    @Query("SELECT t.tenantId FROM TenantAdminAssignment t WHERE t.userId = :userId")
    Optional<UUID> findTenantIdByUserId(@Param("userId") UUID userId);

    void deleteByUserId(UUID userId);
}
