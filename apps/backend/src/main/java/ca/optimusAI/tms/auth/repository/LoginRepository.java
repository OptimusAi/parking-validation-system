package ca.optimusAI.tms.auth.repository;

import ca.optimusAI.tms.auth.entity.Login;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoginRepository extends JpaRepository<Login, String> {

    /** Look up by the identity-provider's subject/user ID. */
    Optional<Login> findByProviderUserIdIgnoreCase(String providerUserId);
}

