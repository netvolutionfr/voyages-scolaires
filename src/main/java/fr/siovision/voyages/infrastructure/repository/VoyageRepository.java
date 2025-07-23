package fr.siovision.voyages.infrastructure.repository;

import fr.siovision.voyages.domain.model.Voyage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoyageRepository extends JpaRepository<Voyage, Long> {
}
