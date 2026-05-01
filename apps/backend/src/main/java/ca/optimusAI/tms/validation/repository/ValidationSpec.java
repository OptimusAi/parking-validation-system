package ca.optimusAI.tms.validation.repository;

import ca.optimusAI.tms.validation.entity.ValidationSession;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA Specification builders for ValidationSession queries.
 *
 * Using Specifications (Criteria API) instead of JPQL avoids the
 * PostgreSQL "could not determine data type of parameter $N" error
 * that occurs when null bind parameters are used in "? IS NULL" checks.
 */
public final class ValidationSpec {

    private ValidationSpec() {}

    /**
     * Specification for the standard list endpoint.
     * Tenant isolation is handled by the Hibernate tenantFilter applied
     * by the MVC interceptor; this spec only adds the user-supplied filters.
     */
    public static Specification<ValidationSession> filtered(
            String status,
            UUID zoneId,
            String licensePlate,
            Instant from,
            Instant to) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("isDeleted")));

            if (status != null)       predicates.add(cb.equal(root.get("status"),       status));
            if (zoneId != null)       predicates.add(cb.equal(root.get("zoneId"),        zoneId));
            if (licensePlate != null) predicates.add(cb.equal(root.get("licensePlate"),  licensePlate));
            if (from != null)         predicates.add(cb.greaterThanOrEqualTo(root.get("startTime"), from));
            if (to != null)           predicates.add(cb.lessThanOrEqualTo(root.get("startTime"),   to));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Specification for the report worker, which runs outside an HTTP session
     * (no Hibernate tenantFilter active). The tenantId predicate is therefore
     * added explicitly.
     */
    public static Specification<ValidationSession> forReport(
            UUID tenantId,
            String status,
            UUID zoneId,
            Instant from,
            Instant to) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));
            predicates.add(cb.isFalse(root.get("isDeleted")));

            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (zoneId != null) predicates.add(cb.equal(root.get("zoneId"), zoneId));
            if (from != null)   predicates.add(cb.greaterThanOrEqualTo(root.get("startTime"), from));
            if (to != null)     predicates.add(cb.lessThanOrEqualTo(root.get("startTime"),   to));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

