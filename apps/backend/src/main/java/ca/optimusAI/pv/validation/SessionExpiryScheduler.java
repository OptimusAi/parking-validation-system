package ca.optimusAI.pv.validation;

import ca.optimusAI.pv.validation.entity.ValidationSession;
import ca.optimusAI.pv.validation.repository.ValidationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Periodically scans for ACTIVE sessions whose end_time is within the next
 * 30 minutes and publishes a SESSION_EXPIRING_SOON event for each one.
 *
 * Runs every 60 seconds. Uses a native query to bypass the Hibernate tenant
 * filter — the scheduler needs to check all tenants.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionExpiryScheduler {

    private final ValidationRepository validationRepository;
    private final ValidationService validationService;

    @Scheduled(fixedDelay = 60_000)
    public void checkExpiringSessions() {
        Instant now    = Instant.now();
        Instant cutoff = now.plusSeconds(30 * 60);

        List<ValidationSession> expiring = validationRepository.findExpiringSoon(now, cutoff);

        if (!expiring.isEmpty()) {
            log.info("SessionExpiryScheduler: {} session(s) expiring within 30 min", expiring.size());
            for (ValidationSession session : expiring) {
                try {
                    validationService.publishExpiringSoon(session);
                } catch (Exception e) {
                    log.error("Failed to publish expiry event for session {}: {}",
                            session.getId(), e.getMessage());
                }
            }
        }
    }
}
