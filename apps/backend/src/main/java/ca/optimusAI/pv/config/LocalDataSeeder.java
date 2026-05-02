package ca.optimusAI.pv.config;

import ca.optimusAI.pv.tenant.entity.ClientAdminAssignment;
import ca.optimusAI.pv.tenant.entity.SubTenantAdminAssignment;
import ca.optimusAI.pv.tenant.entity.TenantAdminAssignment;
import ca.optimusAI.pv.tenant.repository.ClientAdminAssignmentRepository;
import ca.optimusAI.pv.tenant.repository.SubTenantAdminAssignmentRepository;
import ca.optimusAI.pv.tenant.repository.TenantAdminAssignmentRepository;
import ca.optimusAI.pv.user.entity.AppUser;
import ca.optimusAI.pv.user.entity.UserRole;
import ca.optimusAI.pv.user.repository.AppUserRepository;
import ca.optimusAI.pv.user.repository.UserRoleRepository;
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
 * Seeds local development users on startup (profile=local).
 *
 * Creates AppUser + UserRole rows for each seed entry.
 * All seeded users share the password {@code Admin1234!}.
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalDataSeeder implements ApplicationRunner {

    private static final UUID CLIENT_ID     = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_ID     = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SUB_TENANT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private final AppUserRepository                 userRepository;
    private final UserRoleRepository                 userRoleRepository;
    private final ClientAdminAssignmentRepository    clientAdminAssignmentRepository;
    private final TenantAdminAssignmentRepository    tenantAdminAssignmentRepository;
    private final SubTenantAdminAssignmentRepository subTenantAdminAssignmentRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("[LocalDataSeeder] Seeding local test users");

        List<UserSeed> seeds = List.of(
            new UserSeed("local|venu-kannuri-admin",
                         "venu.kannuri@optimus-ai.com",
                         "Venu", "Kannuri",
                         "ADMIN", null, null, null),

            new UserSeed("local|venu-kannuri-client-admin",
                         "venukannuri.cloud@gmail.com",
                         "Venu", "Kannuri (Cloud)",
                         "CLIENT_ADMIN", CLIENT_ID, null, null),

            new UserSeed("local|cpatest1100-tenant-admin",
                         "cpatest1100@gmail.com",
                         "CPA Test", "Tenant Admin",
                         "TENANT_ADMIN", CLIENT_ID, TENANT_ID, null),

            new UserSeed("local|cpatest8963-subtenant-user",
                         "cpatest8963@gmail.com",
                         "CPA Test", "SubTenant User",
                         "SUB_TENANT_ADMIN", CLIENT_ID, TENANT_ID, SUB_TENANT_ID)
        );

        for (UserSeed seed : seeds) {
            AppUser user = userRepository.findByAuthProviderUserId(seed.authProviderUserId())
                .orElseGet(() -> {
                    AppUser newUser = AppUser.builder()
                            .authProviderUserId(seed.authProviderUserId())
                            .email(seed.email())
                            .firstName(seed.firstName())
                            .lastName(seed.lastName())
                            .isActive(true)
                            .build();
                    AppUser saved = userRepository.save(newUser);
                    log.info("[LocalDataSeeder] Created user {} ({} {})", seed.email(), seed.firstName(), seed.lastName());
                    return saved;
                });

            // Upsert UserRole (role only — scope lives in assignment tables)
            UserRole userRole = userRoleRepository.findByUserId(user.getId())
                .orElseGet(() -> UserRole.builder().userId(user.getId()).build());

            if (!seed.role().equals(userRole.getRole()) || userRole.getId() == null) {
                userRole.setRole(seed.role());
                userRoleRepository.save(userRole);
                log.info("[LocalDataSeeder] Upserted role={} for {}", seed.role(), seed.email());
            } else {
                log.debug("[LocalDataSeeder] User already seeded: {}", seed.email());
            }

            // Seed assignment table based on role
            switch (seed.role()) {
                case "CLIENT_ADMIN" -> {
                    if (seed.clientId() != null && seed.tenantId() != null) {
                        clientAdminAssignmentRepository.findByUserIdAndTenantId(user.getId(), seed.tenantId())
                            .ifPresentOrElse(
                                existing -> {},
                                () -> clientAdminAssignmentRepository.save(
                                        ClientAdminAssignment.builder()
                                                .userId(user.getId())
                                                .clientId(seed.clientId())
                                                .tenantId(seed.tenantId())
                                                .build())
                            );
                    }
                }
                case "TENANT_ADMIN" -> {
                    if (seed.tenantId() != null) {
                        tenantAdminAssignmentRepository.findByUserId(user.getId()).ifPresentOrElse(
                            existing -> {
                                if (!seed.tenantId().equals(existing.getTenantId())) {
                                    existing.setTenantId(seed.tenantId());
                                    tenantAdminAssignmentRepository.save(existing);
                                }
                            },
                            () -> tenantAdminAssignmentRepository.save(
                                    TenantAdminAssignment.builder()
                                            .userId(user.getId())
                                            .tenantId(seed.tenantId())
                                            .build())
                        );
                    }
                }
                case "SUB_TENANT_ADMIN" -> {
                    if (seed.tenantId() != null && seed.subTenantId() != null) {
                        subTenantAdminAssignmentRepository.findByUserId(user.getId()).ifPresentOrElse(
                            existing -> {
                                existing.setTenantId(seed.tenantId());
                                existing.setSubTenantId(seed.subTenantId());
                                subTenantAdminAssignmentRepository.save(existing);
                            },
                            () -> subTenantAdminAssignmentRepository.save(
                                    SubTenantAdminAssignment.builder()
                                            .userId(user.getId())
                                            .tenantId(seed.tenantId())
                                            .subTenantId(seed.subTenantId())
                                            .build())
                        );
                    }
                }
            }
        }

        log.info("[LocalDataSeeder] Done seeding local test users.");
    }

    private record UserSeed(
            String authProviderUserId,
            String email,
            String firstName,
            String lastName,
            String role,
            UUID clientId,
            UUID tenantId,
            UUID subTenantId
    ) {}
}
