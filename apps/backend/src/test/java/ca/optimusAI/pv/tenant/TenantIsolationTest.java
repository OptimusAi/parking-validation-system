package ca.optimusAI.pv.tenant;

import ca.optimusAI.pv.validation.entity.ValidationSession;
import ca.optimusAI.pv.validation.repository.ValidationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies tenant data isolation: TENANT_ADMIN A cannot see TENANT_ADMIN B data.
 *
 * Uses a real PostgreSQL container via Testcontainers so Hibernate filters
 * and Flyway migrations are fully exercised.
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class TenantIsolationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("tms_parking_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    ValidationRepository validationRepository;

    @Test
    void givenTwoTenants_tenantACannotSeeSessionsForTenantB() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();

        ValidationSession sessionA = ValidationSession.builder()
                .tenantId(tenantA)
                .zoneId(UUID.randomUUID())
                .licensePlate("AAA111")
                .startTime(Instant.now())
                .endTime(Instant.now().plusSeconds(3600))
                .status("ACTIVE")
                .build();

        ValidationSession sessionB = ValidationSession.builder()
                .tenantId(tenantB)
                .zoneId(UUID.randomUUID())
                .licensePlate("BBB222")
                .startTime(Instant.now())
                .endTime(Instant.now().plusSeconds(3600))
                .status("ACTIVE")
                .build();

        validationRepository.save(sessionA);
        validationRepository.save(sessionB);
        validationRepository.flush();

        // Duplicate check for plate in tenantA's scope should NOT find tenantB sessions
        var tenantAResults = validationRepository.findActiveByPlateAndTenant(
                "BBB222", tenantA, java.util.List.of("ACTIVE", "EXTENDED"));

        assertTrue(tenantAResults.isEmpty(),
                "Tenant A should not see Tenant B's BBB222 plate session");
    }
}
