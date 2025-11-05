package fr.siovision.voyages.infrastructure.repository;

import fr.siovision.voyages.domain.model.StudentHealthForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface HealthFormAdminRepository extends JpaRepository<StudentHealthForm, UUID> {

    interface HealthFormRow {
        UUID getId();
        Long getStudentId();
        String getPayload();
        LocalDateTime getSignedAt();
        LocalDateTime getValidUntil();
        LocalDateTime getCreatedAt();
        LocalDateTime getUpdatedAt();
        Long getVersion();
    }

    /**
     * Dernière fiche par élève (on prend la version la plus haute, puis updated_at).
     * NB: si "version" est strictement monotone, ORDER BY version DESC suffit.
     */
    @Query(value = """
        SELECT id, student_id AS studentId, payload,
               signed_at AS signedAt, valid_until AS validUntil, 
               created_at AS createdAt, updated_at AS updatedAt, 
               version
        FROM student_health_form
        WHERE student_id = :userId
        ORDER BY version DESC, updated_at DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<HealthFormRow> findLatestByStudentId(@Param("userId") Long userId);

    @Query(value = """
        SELECT EXISTS(
            SELECT 1 FROM student_health_form WHERE student_id = :userId
        )
        """, nativeQuery = true)
    boolean existsByStudentIdNative(@Param("userId") Long userId);
}