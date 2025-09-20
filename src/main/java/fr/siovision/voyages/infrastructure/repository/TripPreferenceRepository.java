package fr.siovision.voyages.infrastructure.repository;

import fr.siovision.voyages.domain.model.TripPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TripPreferenceRepository extends JpaRepository<TripPreference, Long> {
    Optional<TripPreference> findByTripIdAndUserPublicId(Long voyageId, UUID publicId);

    long countByTripIdAndInterest(Long tripId, String interest);
}
