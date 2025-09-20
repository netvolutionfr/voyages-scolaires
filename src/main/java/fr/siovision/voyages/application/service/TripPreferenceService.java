package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.domain.model.Trip;
import fr.siovision.voyages.domain.model.TripPreference;
import fr.siovision.voyages.infrastructure.repository.UserRepository;
import fr.siovision.voyages.infrastructure.repository.TripPreferenceRepository;
import fr.siovision.voyages.infrastructure.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TripPreferenceService {
    private final TripPreferenceRepository tripPreferenceRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;

    public String getVoyagePref(Jwt jwt, Long tripId) {
        String keycloakId = jwt.getSubject();
        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + keycloakId));

        return tripPreferenceRepository.findByTripIdAndUserPublicId(tripId, user.getPublicId())
                .map(TripPreference::getInterest)
                .orElse(null); // Retourne null si aucune préférence n'est trouvée
    }

    public boolean isInterested(User user, Long voyageId) {
        return tripPreferenceRepository.findByTripIdAndUserPublicId(voyageId, user.getPublicId())
                .map(voyagePreference -> "Yes".equalsIgnoreCase(voyagePreference.getInterest()))
                .orElse(false); // Retourne false si aucune préférence n'est trouvée
    }

    public long countInterestedUsers(Long voyageId) {
        return tripPreferenceRepository.countByTripIdAndInterest(voyageId, "YES");
    }

    public boolean updateVoyagePref(Jwt jwt, Long tripId, String interest) {
        String keycloakId = jwt.getSubject();
        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + keycloakId));

        TripPreference trippreference = tripPreferenceRepository.findByTripIdAndUserPublicId(tripId, user.getPublicId())
                .orElseGet(() -> {
                    TripPreference newPref = new TripPreference();
                    newPref.setUser(user);
                    Trip trip = tripRepository.findById(tripId)
                            .orElseThrow(() -> new IllegalArgumentException("Voyage not found with id: " + tripId));
                    newPref.setTrip(trip);
                    return newPref;
                });
        trippreference.setInterest(interest);
        tripPreferenceRepository.save(trippreference);
        return true;
    }
}
