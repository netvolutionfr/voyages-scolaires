package fr.siovision.voyages.infrastructure.repository;

import fr.siovision.voyages.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByKeycloakId(String keycloakId);

    @Query("""
                SELECT u
                FROM User u
                WHERE COALESCE(:q, '') = ''
                   OR lower(u.nom) LIKE lower(concat('%', :q, '%'))
            """)
    Page<User> search(@Param("q") String q, Pageable pageable);

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);
}
