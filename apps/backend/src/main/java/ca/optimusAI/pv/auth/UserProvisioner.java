package ca.optimusAI.pv.auth;

import ca.optimusAI.pv.user.entity.AppUser;
import ca.optimusAI.pv.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates or fetches a User record in its own database transaction (REQUIRES_NEW).
 *
 * Using a separate transaction isolates the INSERT so that a
 * DataIntegrityViolationException caused by concurrent first-logins can be
 * caught and recovered without marking the outer transaction rollback-only.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserProvisioner {

    private final AppUserRepository userRepository;

    /**
     * Returns the existing user for {@code authProviderUserId}, or creates a new one
     * with role USER if this is the user's very first login.
     *
     * <p>If two requests race for the same new user, the losing thread catches
     * the duplicate-key exception and re-fetches the record the winning thread
     * already committed.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AppUser findOrCreate(String authProviderUserId, String email, String name) {
        return userRepository.findByAuthProviderUserId(authProviderUserId).orElseGet(() -> {
            log.info("First login for authProviderUserId={}; creating USER", authProviderUserId);
            AppUser newUser = AppUser.builder()
                    .authProviderUserId(authProviderUserId)
                    .email(email)
                    .name(name)
                    .role("USER")
                    .isActive(true)
                    .build();
            try {
                return userRepository.saveAndFlush(newUser);
            } catch (DataIntegrityViolationException e) {
                // Race condition: another thread already committed this user.
                log.debug("Concurrent first-login for authProviderUserId={}; re-fetching existing record",
                        authProviderUserId);
                return userRepository.findByAuthProviderUserId(authProviderUserId)
                        .orElseThrow(() -> new IllegalStateException(
                                "User absent after duplicate-key violation for: " + authProviderUserId, e));
            }
        });
    }
}

