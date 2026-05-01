package ca.optimusAI.pv.validation.repository;

import ca.optimusAI.pv.validation.entity.ValidationSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The two previous JPQL methods (findFiltered / findForReport) that used
 * "(:param IS NULL OR col = :param)" were removed because PostgreSQL cannot
 * determine the type of a null bind parameter in a "? IS NULL" check, causing:
 *   "could not determine data type of parameter $N"
 *
 * They are replaced by JpaSpecificationExecutor + ValidationSpec, which builds
 * predicates only for non-null values and therefore never sends an untyped null
 * parameter to PostgreSQL.
 */
@Repository
public interface ValidationRepository
        extends JpaRepository<ValidationSession, UUID>,
                JpaSpecificationExecutor<ValidationSession> {

    Optional<ValidationSession> findByIdAndIsDeletedFalse(UUID id);

    /**
     * Used by SessionExpiryScheduler — finds ACTIVE sessions whose end_time
     * is between now and now+30 min (across all tenants — no filter applied here).
     */
    @Query(value = """
        SELECT * FROM validation_sessions
        WHERE status = 'ACTIVE'
          AND is_deleted = false
          AND end_time BETWEEN CAST(:now AS timestamptz) AND CAST(:cutoff AS timestamptz)
        """, nativeQuery = true)
    List<ValidationSession> findExpiringSoon(
            @Param("now") Instant now,
            @Param("cutoff") Instant cutoff);
}
