package fr.siovision.voyages.infrastructure.repository;

import fr.siovision.voyages.domain.model.Participant;
import fr.siovision.voyages.domain.model.Trip;
import fr.siovision.voyages.domain.model.TripParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripParticipantRepository extends JpaRepository<TripParticipant, Integer> {
    boolean existsByTripAndParticipant(Trip trip, Participant participant);
}
