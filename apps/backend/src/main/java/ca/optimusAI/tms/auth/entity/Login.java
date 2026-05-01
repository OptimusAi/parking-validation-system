package ca.optimusAI.tms.auth.entity;

import ca.optimusAI.tms.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Persisted record of each identity-provider account that has ever
 * authenticated with TMS.  One row per (loginProvider, providerUserId) pair.
 *
 * Mirrors the {@code login} table from the legacy CPA TMS system.
 */
@Entity
@Table(
    name = "login",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_login_provider_user_id",
        columnNames = {"login_provider", "provider_user_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Login {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "login_provider", nullable = false, length = 128)
    private String loginProvider;

    @Column(name = "provider_user_id", nullable = false, length = 256)
    private String providerUserId;

    @Column(length = 128)
    private String email;

    @Column(name = "email_prefix", length = 256)
    private String emailPrefix;

    @Column(name = "last_login_date", nullable = false)
    private Instant lastLoginDate;

    /**
     * The TMS application user this login is linked to.
     * May be null transiently while the user is being created.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "login_user_fk"))
    private User user;

    /** Stores the OAuth gtoken/access-token returned by the identity provider. */
    @Column(columnDefinition = "TEXT")
    private String gtoken;

    // ── Convenience factory ────────────────────────────────────────────────────

    /**
     * Build a brand-new Login from OAuth claims.
     * The caller is responsible for linking {@code user} before persisting.
     */
    public static Login create(String loginProvider, String providerUserId, String email, String gtoken) {
        String emailPrefix = null;
        if (email != null) {
            int at = email.indexOf('@');
            emailPrefix = (at > 0) ? email.substring(0, at) : email;
        }
        return Login.builder()
                .id(java.util.UUID.randomUUID().toString())
                .loginProvider(loginProvider)
                .providerUserId(providerUserId)
                .email(email)
                .emailPrefix(emailPrefix)
                .lastLoginDate(Instant.now())
                .gtoken(gtoken)
                .build();
    }

    /** Update mutable fields on a subsequent login. */
    public void updateOnLogin(String email, String gtoken) {
        this.email = email;
        if (email != null) {
            int at = email.indexOf('@');
            this.emailPrefix = (at > 0) ? email.substring(0, at) : email;
        }
        this.gtoken = gtoken;
        this.lastLoginDate = Instant.now();
    }
}

