package ca.optimusAI.pv.report.service;

import ca.optimusAI.pv.report.entity.ReportJob;
import ca.optimusAI.pv.report.repository.ReportJobRepository;
import ca.optimusAI.pv.shared.PageResponse;
import ca.optimusAI.pv.shared.TenantContext;
import ca.optimusAI.pv.shared.exception.ResourceNotFoundException;
import ca.optimusAI.pv.shared.exception.UnauthorizedTenantAccessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportJobRepository reportJobRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public ReportJob queueReport(String reportType, String format,
                                  Map<String, Object> filters,
                                  UUID tenantId, UUID clientId, UUID requestedBy) {
        String filtersJson;
        try {
            filtersJson = objectMapper.writeValueAsString(filters != null ? filters : Map.of());
        } catch (JsonProcessingException e) {
            filtersJson = "{}";
        }

        ReportJob job = ReportJob.builder()
                .tenantId(tenantId)
                .clientId(clientId)
                .reportType(reportType.toUpperCase())
                .format(format.toUpperCase())
                .filters(filtersJson)
                .status("QUEUED")
                .requestedBy(requestedBy)
                .build();

        job = reportJobRepository.save(job);

        rabbitTemplate.convertAndSend("reports.generate", "reports.generate",
                new ReportJobMessage(job.getId()));
        log.info("Queued report job={} type={} format={} tenantId={}", job.getId(), reportType, format, tenantId);
        return job;
    }

    @Transactional(readOnly = true)
    public ReportJob getJob(UUID jobId) {
        ReportJob job = reportJobRepository.findByIdAndIsDeletedFalse(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Report job not found: " + jobId));
        assertTenantAccess(job.getTenantId());
        return job;
    }

    @Transactional(readOnly = true)
    public PageResponse<ReportJob> list(int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("createdAt").descending());
        if (TenantContext.hasRole("ADMIN")) {
            return PageResponse.of(reportJobRepository.findAll(pr));
        }
        UUID tenantId = TenantContext.tenantId();
        if (tenantId == null) {
            throw new UnauthorizedTenantAccessException("No tenant context");
        }
        return PageResponse.of(reportJobRepository.findByTenantIdAndIsDeletedFalse(tenantId, pr));
    }

    private void assertTenantAccess(UUID jobTenantId) {
        if (TenantContext.hasRole("ADMIN")) return;
        UUID tenantId = TenantContext.tenantId();
        if (!jobTenantId.equals(tenantId)) {
            throw new UnauthorizedTenantAccessException("Access denied to report job");
        }
    }

    public record ReportJobMessage(UUID jobId) {}
}
