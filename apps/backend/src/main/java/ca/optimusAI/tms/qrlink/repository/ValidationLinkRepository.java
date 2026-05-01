package ca.optimusAI.tms.qrlink.repository;

import ca.optimusAI.tms.qrlink.entity.ValidationLink;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ValidationLinkRepository extends JpaRepository<ValidationLink, UUID> {

    /** Filtered by tenantFilter (Hibernate filter applied by interceptor). */
    Page<ValidationLink> findAll(Pageable pageable);

    /** Filtered fetch by id — used for authorized operations. */
    Optional<ValidationLink> findByIdAndIsDeletedFalse(UUID id);

    /**
     * Token lookup bypasses the Hibernate tenant filter on purpose:
     * this is the public endpoint — no tenant context when a visitor scans.
     * Checks is_deleted = false and is_active = true directly in the query.
     */
    @Query("SELECT l FROM ValidationLink l " +
           "WHERE l.token = :token AND l.isDeleted = false AND l.isActive = true")
    Optional<ValidationLink> findActiveByToken(@Param("token") String token);

    /** Increment scan count atomically. */
    @Modifying
    @Query("UPDATE ValidationLink l SET l.scanCount = l.scanCount + 1 WHERE l.id = :id")
    void incrementScanCount(@Param("id") UUID id);
}
