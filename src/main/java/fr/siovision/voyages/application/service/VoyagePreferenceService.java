package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.domain.model.Voyage;
import fr.siovision.voyages.domain.model.VoyagePreference;
import fr.siovision.voyages.infrastructure.repository.UserRepository;
import fr.siovision.voyages.infrastructure.repository.VoyagePreferenceRepository;
import fr.siovision.voyages.infrastructure.repository.VoyageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VoyagePreferenceService {
    private final VoyagePreferenceRepository voyagePreferenceRepository;
    private final UserRepository userRepository;
    private final VoyageRepository voyageRepository;

    public String getVoyagePref(Jwt jwt, Long voyageId) {
        String keycloakId = jwt.getSubject();
        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + keycloakId));

        return voyagePreferenceRepository.findByVoyageIdAndUserPublicId(voyageId, user.getPublicId())
                .map(VoyagePreference::getInterest)
                .orElse(null); // Retourne null si aucune préférence n'est trouvée
    }

    public boolean isInterested(User user, Long voyageId) {
        return voyagePreferenceRepository.findByVoyageIdAndUserPublicId(voyageId, user.getPublicId())
                .map(voyagePreference -> "Yes".equalsIgnoreCase(voyagePreference.getInterest()))
                .orElse(false); // Retourne false si aucune préférence n'est trouvée
    }

    public long countInterestedUsers(Long voyageId) {
        return voyagePreferenceRepository.countByVoyageIdAndInterest(voyageId, "YES");
    }

    public boolean updateVoyagePref(Jwt jwt, Long voyageId, String interest) {
        String keycloakId = jwt.getSubject();
        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + keycloakId));

        VoyagePreference voyagepreference = voyagePreferenceRepository.findByVoyageIdAndUserPublicId(voyageId, user.getPublicId())
                .orElseGet(() -> {
                    VoyagePreference newPref = new VoyagePreference();
                    newPref.setUser(user);
                    Voyage voyage = voyageRepository.findById(voyageId)
                            .orElseThrow(() -> new IllegalArgumentException("Voyage not found with id: " + voyageId));
                    newPref.setVoyage(voyage);
                    return newPref;
                });
        voyagepreference.setInterest(interest);
        voyagePreferenceRepository.save(voyagepreference);
        return true;
    }
}
