package fr.siovision.voyages.infrastructure.repository;

import fr.siovision.voyages.domain.model.RegistrationAttempt;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.Optional;

public interface RegistrationAttemptRepository extends JpaRepository<RegistrationAttempt, Long> {
    Optional<RegistrationAttempt> findByUuid(UUID id);

     Optional<RegistrationAttempt> findByChallenge(@NotNull byte[] value);
}
