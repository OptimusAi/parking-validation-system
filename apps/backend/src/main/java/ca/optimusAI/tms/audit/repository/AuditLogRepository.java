package ca.optimusAI.tms.audit.repository;

import ca.optimusAI.tms.audit.entity.AuditLog;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.Repository;

import java.util.UUID;

public interface AuditLogRepository extends Repository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

    <S extends AuditLog> S save(S entity);

    @Override
    default long delete(Specification<AuditLog> spec) {
        throw new UnsupportedOperationException("AuditLog is append-only — delete is not permitted");
    }
}
