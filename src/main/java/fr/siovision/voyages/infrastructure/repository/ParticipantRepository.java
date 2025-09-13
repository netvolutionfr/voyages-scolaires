package fr.siovision.voyages.infrastructure.repository;

import fr.siovision.voyages.domain.model.Participant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ParticipantRepository extends JpaRepository<Participant, UUID> {
    Optional<Participant> findByEmail(String email);

    Optional<Participant> findByStudentAccountId(Long id);

    Optional<Participant> findByEmailIgnoreCase(String email);
    @Query("""
                SELECT p
                FROM Participant p
                WHERE COALESCE(:q, '') = ''
                   OR lower(p.prenom) LIKE lower(concat('%', :q, '%'))
            """)
    Page<Participant> search(@Param("q") String q, Pageable pageable);

    Optional<Participant> findByPublicId(UUID participantId);

    Optional<Participant> findByStudentAccount_Email(String studentEmail);
}
