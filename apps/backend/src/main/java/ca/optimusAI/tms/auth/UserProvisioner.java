package ca.optimusAI.tms.auth;

import ca.optimusAI.tms.user.entity.User;
import ca.optimusAI.tms.user.repository.UserRepository;
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

    private final UserRepository userRepository;

    /**
     * Returns the existing user for {@code auth0UserId}, or creates a new one
     * with role SUBTENANT_USER if this is the user's very first login.
     *
     * <p>If two requests race for the same new user, the losing thread catches
     * the duplicate-key exception and re-fetches the record the winning thread
     * already committed.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public User findOrCreate(String auth0UserId, String email, String name) {
        return userRepository.findByAuth0UserId(auth0UserId).orElseGet(() -> {
            log.info("First login for auth0UserId={}; creating SUBTENANT_USER", auth0UserId);
            User newUser = User.builder()
                    .auth0UserId(auth0UserId)
                    .email(email)
                    .name(name)
                    .role("SUBTENANT_USER")
                    .isActive(true)
                    .build();
            try {
                return userRepository.saveAndFlush(newUser);
            } catch (DataIntegrityViolationException e) {
                // Race condition: another thread already committed this user.
                // Simply re-fetch the record it created.
                log.debug("Concurrent first-login for auth0UserId={}; re-fetching existing record",
                        auth0UserId);
                return userRepository.findByAuth0UserId(auth0UserId)
                        .orElseThrow(() -> new IllegalStateException(
                                "User absent after duplicate-key violation for: " + auth0UserId, e));
            }
        });
    }
}

