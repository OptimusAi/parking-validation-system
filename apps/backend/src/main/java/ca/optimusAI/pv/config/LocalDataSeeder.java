package ca.optimusAI.pv.config;

import ca.optimusAI.pv.user.entity.User;
import ca.optimusAI.pv.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Seeds local development users on startup.
 *
 * Active only when the "local" Spring profile is set (e.g. SPRING_PROFILES_ACTIVE=local).
 *
 * Auth0 user IDs use the placeholder prefix "local|" — after a real first-login the
 * auth0_user_id in the users table should be updated to match the actual Auth0 subject.
 *
 * UUIDs are tied to the seed data already inserted by V3__seed_data.sql:
 *   client    : 11111111-1111-1111-1111-111111111111  (CPA Demo Client)
 *   tenant    : 22222222-2222-2222-2222-222222222222  (Downtown Parking)
 *   sub-tenant: aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa  (Level 1 Retail)
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalDataSeeder implements ApplicationRunner {

    private static final UUID CLIENT_ID     = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_ID     = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SUB_TENANT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("[LocalDataSeeder] Seeding local test users ...");

        List<UserSeed> seeds = List.of(
            new UserSeed("local|venu-kannuri-admin",
                         "venu.kannuri@optimus-ai.com",
                         "Venu Kannuri",
                         "ADMIN",
                         null, null, null),

            new UserSeed("local|venu-kannuri-client-admin",
                         "venukannuri.cloud@gmail.com",
                         "Venu Kannuri (Cloud)",
                         "CLIENT_ADMIN",
                         CLIENT_ID, null, null),

            new UserSeed("local|cpatest1100-tenant-admin",
                         "cpatest1100@gmail.com",
                         "CPA Test Tenant Admin",
                         "TENANT_ADMIN",
                         CLIENT_ID, TENANT_ID, null),

            new UserSeed("local|cpatest8963-subtenant-user",
                         "cpatest8963@gmail.com",
                         "CPA Test SubTenant User",
                         "SUBTENANT_USER",
                         CLIENT_ID, TENANT_ID, SUB_TENANT_ID)
        );

        for (UserSeed seed : seeds) {
            userRepository.findByAuth0UserId(seed.auth0UserId()).ifPresentOrElse(
                existing -> {
                    // Keep existing record but ensure role is correct
                    if (!existing.getRole().equals(seed.role())) {
                        existing.setRole(seed.role());
                        userRepository.save(existing);
                        log.info("[LocalDataSeeder] Updated role for {} → {}", seed.email(), seed.role());
                    } else {
                        log.debug("[LocalDataSeeder] User already seeded: {}", seed.email());
                    }
                },
                () -> {
                    User user = User.builder()
                            .auth0UserId(seed.auth0UserId())
                            .email(seed.email())
                            .name(seed.name())
                            .role(seed.role())
                            .clientId(seed.clientId())
                            .tenantId(seed.tenantId())
                            .subTenantId(seed.subTenantId())
                            .isActive(true)
                            .build();
                    userRepository.save(user);
                    log.info("[LocalDataSeeder] Created user {} with role {}", seed.email(), seed.role());
                }
            );
        }

        log.info("[LocalDataSeeder] Done seeding local test users.");
    }

    private record UserSeed(
            String auth0UserId,
            String email,
            String name,
            String role,
            UUID clientId,
            UUID tenantId,
            UUID subTenantId
    ) {}
}
