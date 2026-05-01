package ca.optimusAI.pv.user.repository;

import ca.optimusAI.pv.user.entity.AppUser;
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
public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByAuthProviderUserId(String authProviderUserId);

    Page<AppUser> findByClientId(UUID clientId, Pageable pageable);

    @Query("SELECT u FROM AppUser u WHERE u.tenantId IN :tenantIds")
    Page<AppUser> findByTenantIdIn(@Param("tenantIds") List<UUID> tenantIds, Pageable pageable);
}
