package ca.optimusAI.tms.report.worker;

import ca.optimusAI.tms.report.entity.ReportJob;
import ca.optimusAI.tms.report.repository.ReportJobRepository;
import ca.optimusAI.tms.report.service.ReportFileGenerator;
import ca.optimusAI.tms.report.service.ReportService.ReportJobMessage;
import ca.optimusAI.tms.tenant.entity.SubTenant;
import ca.optimusAI.tms.tenant.entity.Zone;
import ca.optimusAI.tms.tenant.repository.SubTenantRepository;
import ca.optimusAI.tms.tenant.repository.ZoneRepository;
import ca.optimusAI.tms.tenant.service.AwsS3Service;
import ca.optimusAI.tms.validation.entity.ValidationSession;
import ca.optimusAI.tms.validation.repository.ValidationRepository;
import ca.optimusAI.tms.validation.repository.ValidationSpec;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportWorker {

    private final ReportJobRepository reportJobRepository;
    private final ValidationRepository validationRepository;
    private final ZoneRepository zoneRepository;
    private final SubTenantRepository subTenantRepository;
    private final ReportFileGenerator fileGenerator;
    private final AwsS3Service awsS3Service;
    private final ObjectMapper objectMapper;

    private static final int PAGE_SIZE = 500;

    @RabbitListener(queues = "reports.generate")
    public void processReport(ReportJobMessage message) {
        UUID jobId = message.jobId();
        log.info("Processing report job={}", jobId);

        ReportJob job = markProcessing(jobId);
        if (job == null) {
            log.warn("Report job not found: {}", jobId);
            return;
        }

        try {
            Map<String, Object> filters = parseFilters(job.getFilters());

            // ── 2. Query all validation sessions (paged, explicit tenantId) ──────
            List<ValidationSession> sessions = fetchAllSessions(job.getTenantId(), filters);
            log.debug("Fetched {} sessions for report job={}", sessions.size(), jobId);

            // ── 3. Build lookup maps for zone and sub-tenant names ───────────────
            Set<UUID> zoneIds = sessions.stream().map(ValidationSession::getZoneId)
                    .filter(Objects::nonNull).collect(Collectors.toSet());
            Set<UUID> subTenantIds = sessions.stream().map(ValidationSession::getSubTenantId)
                    .filter(Objects::nonNull).collect(Collectors.toSet());

            Map<UUID, String> zoneNumbers = zoneRepository.findAllByIdIn(zoneIds)
                    .stream().collect(Collectors.toMap(Zone::getId, Zone::getZoneNumber));
            Map<UUID, String> zoneNames = zoneRepository.findAllByIdIn(zoneIds)
                    .stream().collect(Collectors.toMap(Zone::getId, Zone::getName));
            Map<UUID, String> subTenantNames = subTenantRepository.findAllByIdIn(subTenantIds)
                    .stream().collect(Collectors.toMap(SubTenant::getId, SubTenant::getName));

            // ── 4. Generate file bytes ────────────────────────────────────────────
            String format = job.getFormat();
            byte[] fileBytes = switch (format) {
                case "CSV"   -> fileGenerator.generateCsv(sessions, zoneNumbers, zoneNames, subTenantNames);
                case "EXCEL" -> fileGenerator.generateExcel(sessions, zoneNumbers, zoneNames, subTenantNames);
                case "PDF"   -> fileGenerator.generatePdf(sessions, zoneNumbers, zoneNames, subTenantNames);
                default      -> throw new IllegalArgumentException("Unsupported format: " + format);
            };

            // ── 5. Upload to S3 ───────────────────────────────────────────────────
            String ext = switch (format) {
                case "CSV"   -> "csv";
                case "EXCEL" -> "xlsx";
                case "PDF"   -> "pdf";
                default      -> "bin";
            };
            String contentType = switch (format) {
                case "CSV"   -> "text/csv";
                case "EXCEL" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                case "PDF"   -> "application/pdf";
                default      -> "application/octet-stream";
            };

            String s3Key = "reports/" + job.getTenantId() + "/" + jobId + "." + ext;
            awsS3Service.uploadBytes(s3Key, fileBytes, contentType);

            // ── 6. Generate presigned URL (24h) ───────────────────────────────────
            String presignedUrl = awsS3Service.generatePresignedUrl(s3Key, Duration.ofHours(24));

            // ── 7. Mark COMPLETED ─────────────────────────────────────────────────
            markCompleted(jobId, presignedUrl, (long) fileBytes.length,
                    Instant.now().plus(Duration.ofHours(24)));
            log.info("Report job={} COMPLETED size={} url={}", jobId, fileBytes.length, presignedUrl);

        } catch (Exception e) {
            log.error("Report job={} FAILED: {}", jobId, e.getMessage(), e);
            markFailed(jobId, e.getMessage());
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    @Transactional
    public ReportJob markProcessing(UUID jobId) {
        return reportJobRepository.findById(jobId).map(job -> {
            job.setStatus("PROCESSING");
            return reportJobRepository.save(job);
        }).orElse(null);
    }

    @Transactional
    public void markCompleted(UUID jobId, String fileUrl, long fileSizeBytes, Instant expiresAt) {
        reportJobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus("COMPLETED");
            job.setFileUrl(fileUrl);
            job.setFileSizeBytes(fileSizeBytes);
            job.setExpiresAt(expiresAt);
            reportJobRepository.save(job);
        });
    }

    @Transactional
    public void markFailed(UUID jobId, String errorMessage) {
        reportJobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus("FAILED");
            job.setErrorMessage(errorMessage);
            reportJobRepository.save(job);
        });
    }

    private List<ValidationSession> fetchAllSessions(UUID tenantId,
                                                      Map<String, Object> filters) {
        String status    = (String) filters.get("status");
        UUID zoneId      = filters.containsKey("zoneId") && filters.get("zoneId") != null
                ? UUID.fromString(filters.get("zoneId").toString()) : null;
        Instant from     = filters.containsKey("from") && filters.get("from") != null
                ? Instant.parse(filters.get("from").toString()) : null;
        Instant to       = filters.containsKey("to") && filters.get("to") != null
                ? Instant.parse(filters.get("to").toString()) : null;

        List<ValidationSession> all = new ArrayList<>();
        int page = 0;
        Page<ValidationSession> result;
        do {
            result = validationRepository.findAll(
                    ValidationSpec.forReport(tenantId, status, zoneId, from, to),
                    PageRequest.of(page++, PAGE_SIZE));
            all.addAll(result.getContent());
        } while (!result.isLast());

        return all;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseFilters(String filtersJson) {
        try {
            return objectMapper.readValue(filtersJson, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Could not parse filters JSON '{}': {}", filtersJson, e.getMessage());
            return Map.of();
        }
    }
}
