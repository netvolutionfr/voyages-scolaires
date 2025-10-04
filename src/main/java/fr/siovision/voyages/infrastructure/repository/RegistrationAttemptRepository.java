package fr.siovision.voyages.infrastructure.repository;

import fr.siovision.voyages.domain.model.RegistrationAttempt;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface RegistrationAttemptRepository extends JpaRepository<RegistrationAttempt, Long> {
    Optional<RegistrationAttempt> findByUuid(UUID id);

     Optional<RegistrationAttempt> findByChallenge(@NotNull byte[] value);

    int deleteByCreatedAtBefore(LocalDateTime expirationThreshold);

    List<RegistrationAttempt> findByExpiresAtBefore(Instant now);
}
