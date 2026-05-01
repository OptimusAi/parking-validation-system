package ca.optimusAI.tms.report.repository;

import ca.optimusAI.tms.report.entity.ReportJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReportJobRepository extends JpaRepository<ReportJob, UUID> {

    Page<ReportJob> findByTenantIdAndIsDeletedFalse(UUID tenantId, Pageable pageable);

    Optional<ReportJob> findByIdAndIsDeletedFalse(UUID id);
}
