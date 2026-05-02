package ca.optimusAI.pv.tenant.repository;

import ca.optimusAI.pv.tenant.entity.SubTenantAdminAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubTenantAdminAssignmentRepository extends JpaRepository<SubTenantAdminAssignment, UUID> {

    Optional<SubTenantAdminAssignment> findByUserId(UUID userId);

    @Query("SELECT s.tenantId FROM SubTenantAdminAssignment s WHERE s.userId = :userId")
    Optional<UUID> findTenantIdByUserId(@Param("userId") UUID userId);

    @Query("SELECT s.subTenantId FROM SubTenantAdminAssignment s WHERE s.userId = :userId")
    Optional<UUID> findSubTenantIdByUserId(@Param("userId") UUID userId);

    void deleteByUserId(UUID userId);
}
