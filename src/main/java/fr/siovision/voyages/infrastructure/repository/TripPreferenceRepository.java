package fr.siovision.voyages.infrastructure.repository;

import fr.siovision.voyages.domain.model.TripPreference;
import fr.siovision.voyages.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TripPreferenceRepository extends JpaRepository<TripPreference, Long> {
    Optional<TripPreference> findByTripIdAndUserPublicId(Long voyageId, UUID publicId);

    long countByTripIdAndInterest(Long tripId, String interest);

    // Effacement RGPD
    @Modifying
    @Query("delete from TripPreference tp where tp.user = :user")
    void deleteAllByUser(@Param("user") User user);
}
