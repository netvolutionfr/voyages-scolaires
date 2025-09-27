package fr.siovision.voyages.infrastructure.repository;

import fr.siovision.voyages.domain.model.RegistrationAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationAttemptRepository extends JpaRepository<RegistrationAttempt, Long> {
}
