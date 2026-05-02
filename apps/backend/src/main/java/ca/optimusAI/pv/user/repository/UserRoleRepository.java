package ca.optimusAI.pv.user.repository;

import ca.optimusAI.pv.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    Optional<UserRole> findByUserId(UUID userId);

    @Query("SELECT ur.userId FROM UserRole ur WHERE ur.clientId = :clientId")
    List<UUID> findUserIdsByClientId(@Param("clientId") UUID clientId);

    @Query("SELECT ur.userId FROM UserRole ur WHERE ur.tenantId IN :tenantIds")
    List<UUID> findUserIdsByTenantIdIn(@Param("tenantIds") List<UUID> tenantIds);
}
