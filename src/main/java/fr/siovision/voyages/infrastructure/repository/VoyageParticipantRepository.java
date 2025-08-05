package fr.siovision.voyages.infrastructure.repository;

import fr.siovision.voyages.domain.model.Participant;
import fr.siovision.voyages.domain.model.Voyage;
import fr.siovision.voyages.domain.model.VoyageParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoyageParticipantRepository extends JpaRepository<VoyageParticipant, Integer> {
    boolean existsByVoyageAndParticipant(Voyage voyage, Participant participant);
}
