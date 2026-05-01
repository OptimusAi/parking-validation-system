package ca.optimusAI.tms.user.repository;

import ca.optimusAI.tms.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByAuth0UserId(String auth0UserId);
    Page<User> findByClientId(UUID clientId, Pageable pageable);
}
