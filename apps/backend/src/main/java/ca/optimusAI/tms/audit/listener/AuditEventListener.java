package ca.optimusAI.tms.audit.listener;

import ca.optimusAI.tms.audit.entity.AuditLog;
import ca.optimusAI.tms.audit.repository.AuditLogRepository;
import ca.optimusAI.tms.validation.event.ValidationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "audit.all")
    public void onEvent(ValidationEvent event) {
        try {
            String afterState = objectMapper.writeValueAsString(event);
            AuditLog auditLog = AuditLog.builder()
                    .tenantId(event.tenantId())
                    .clientId(event.clientId())
                    .action(event.eventType())
                    .entityType("VALIDATION_SESSION")
                    .entityId(event.sessionId())
                    .afterState(afterState)
                    .build();
            auditLogRepository.save(auditLog);
            log.debug("Audit log written: action={} entityId={}", event.eventType(), event.sessionId());
        } catch (Exception e) {
            log.error("Failed to write audit log for event {}: {}", event.eventType(), e.getMessage());
        }
    }
}
