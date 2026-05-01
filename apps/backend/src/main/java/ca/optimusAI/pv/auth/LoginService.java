package ca.optimusAI.pv.auth;

import ca.optimusAI.pv.auth.entity.Login;
import ca.optimusAI.pv.auth.repository.LoginRepository;
import ca.optimusAI.pv.user.entity.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages the {@code login} table — mirrors the old
 * {@code UserServiceImpl#createAndLogin / loginSuccessful} flow.
 *
 * <p>On every successful OAuth authentication we call {@link #upsertLogin}:
 * <ul>
 *   <li>If a {@code login} row already exists for the {@code providerUserId}:
 *       update last_login_date, gtoken and email, then return the linked
 *       {@code app_user} row (roles are resolved from that user).</li>
 *   <li>If no row exists (first login):
 *       use {@link UserProvisioner} to find-or-create the {@code app_user} row,
 *       create a new {@code login} row, link them, and save both.</li>
 * </ul>
 *
 * The caller ({@code UserRoleService}) can then use the returned {@link AppUser}
 * to build the {@link UserRecord} the way it always did.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginService {

    private final LoginRepository loginRepository;
    private final UserProvisioner  userProvisioner;

    /**
     * Insert-or-update the login record for the given provider identity.
     *
     * @param loginProvider  e.g. "oauth2", "google", "azure-ad"
     * @param providerUserId the subject / uid from the OAuth/OIDC token
     * @param email          email address from the token (may be null)
     * @param displayName    full name for provisioning (may be null)
     * @param gtoken         raw OAuth access-token to store (may be null)
     * @return the {@link AppUser} record linked to (or just created for) this login
     */
    @Transactional
    public AppUser upsertLogin(String loginProvider,
                               String providerUserId,
                               String email,
                               String displayName,
                               String gtoken) {

        return loginRepository.findByProviderUserIdIgnoreCase(providerUserId)
                .map(existing -> {
                    // ── Returning user ────────────────────────────────────────
                    log.debug("Returning login for providerUserId={}, loginId={}",
                            providerUserId, existing.getId());
                    AppUser user = existing.getUser();
                    if (user != null && !user.isActive()) {
                        throw new ca.optimusAI.pv.shared.exception.UnauthorizedTenantAccessException(
                                "User account is disabled");
                    }
                    existing.updateOnLogin(email, gtoken);
                    loginRepository.save(existing);
                    return user;
                })
                .orElseGet(() -> {
                    // ── First login: create user + login row ──────────────────
                    log.info("First login for providerUserId={}; provisioning user", providerUserId);

                    AppUser user = userProvisioner.findOrCreate(providerUserId, email, displayName);

                    Login newLogin = Login.create(loginProvider, providerUserId, email, gtoken);
                    newLogin.setUser(user);
                    loginRepository.save(newLogin);

                    log.info("Created login record {} for userId={}", newLogin.getId(), user.getId());
                    return user;
                });
    }
}
