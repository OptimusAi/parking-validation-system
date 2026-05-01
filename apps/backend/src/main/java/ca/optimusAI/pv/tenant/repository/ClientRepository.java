package ca.optimusAI.pv.tenant.repository;

import ca.optimusAI.pv.tenant.entity.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {

    Page<Client> findAllByIsDeletedFalse(Pageable pageable);

    /** Used by CLIENT_ADMIN — returns a single-item page scoped to their clientId. */
    @Query("SELECT c FROM Client c WHERE c.id = :id AND c.isDeleted = false")
    Page<Client> findByIdAndIsDeletedFalse(@Param("id") UUID id, Pageable pageable);

    boolean existsByIdAndIsDeletedFalse(UUID id);
}
