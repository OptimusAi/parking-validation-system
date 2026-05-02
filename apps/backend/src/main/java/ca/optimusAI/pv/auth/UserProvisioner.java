package ca.optimusAI.pv.auth;

import ca.optimusAI.pv.user.entity.AppUser;
import ca.optimusAI.pv.user.entity.UserRole;
import ca.optimusAI.pv.user.repository.AppUserRepository;
import ca.optimusAI.pv.user.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates or fetches an AppUser record in its own database transaction (REQUIRES_NEW).
 * Also ensures a default UserRole row (role=USER) exists for every new user.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserProvisioner {

    private final AppUserRepository  userRepository;
    private final UserRoleRepository userRoleRepository;

    /**
     * Returns the existing user for {@code authProviderUserId}, or creates a new one
     * with a default USER role if this is the user's very first login.
     *
     * <p>If two requests race for the same new user, the losing thread catches
     * the duplicate-key exception and re-fetches the record the winning thread
     * already committed.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AppUser findOrCreate(String authProviderUserId, String email, String firstName, String lastName) {
        return userRepository.findByAuthProviderUserId(authProviderUserId).orElseGet(() -> {
            log.info("First login for authProviderUserId={}; creating USER", authProviderUserId);
            AppUser newUser = AppUser.builder()
                    .authProviderUserId(authProviderUserId)
                    .email(email)
                    .firstName(firstName)
                    .lastName(lastName)
                    .isActive(true)
                    .build();
            try {
                AppUser saved = userRepository.saveAndFlush(newUser);
                // Create a default USER role row
                userRoleRepository.save(UserRole.builder()
                        .userId(saved.getId())
                        .role("USER")
                        .build());
                return saved;
            } catch (DataIntegrityViolationException e) {
                log.debug("Concurrent first-login for authProviderUserId={}; re-fetching existing record",
                        authProviderUserId);
                return userRepository.findByAuthProviderUserId(authProviderUserId)
                        .orElseThrow(() -> new IllegalStateException(
                                "User absent after duplicate-key violation for: " + authProviderUserId, e));
            }
        });
    }
}

