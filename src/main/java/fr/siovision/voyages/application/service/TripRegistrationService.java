package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.model.TripRegistrationStatus;
import fr.siovision.voyages.domain.model.Trip;
import fr.siovision.voyages.domain.model.TripUser;
import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.infrastructure.dto.TripRegistrationRequest;
import fr.siovision.voyages.infrastructure.dto.TripRegistrationResponse;
import fr.siovision.voyages.infrastructure.repository.TripRepository;
import fr.siovision.voyages.infrastructure.repository.TripUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TripRegistrationService {
    private final CurrentUserService currentUserService;
    private final TripRepository tripRepository;
    private final TripUserRepository tripUserRepository;

    @Transactional
    public TripRegistrationResponse registerCurrentUserForTrip(TripRegistrationRequest req) {
        if (req == null || req.tripId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tripId manquant");
        }
        if (!req.agreeToCommitments()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Vous devez accepter les engagements.");
        }

        final User user = currentUserService.getCurrentUser();
        final Trip trip = tripRepository.findById(req.tripId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Voyage introuvable"));

        // Option: on conserve la fenêtre d'inscription (sinon, commente ces 3 lignes)
        validateRegistrationWindow(trip);

        // Anti-doublon (1 TripUser max par couple)
        if (tripUserRepository.existsByTrip_IdAndUser_Id(trip.getId(), user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vous êtes déjà inscrit à ce voyage.");
        }

        TripUser link = new TripUser();
        link.setTrip(trip);
        link.setUser(user);
        link.setChaperone(false);
        link.setRegistrationDate(LocalDateTime.now());
        link.setRegistrationStatus(TripRegistrationStatus.ENROLLED);

        link = tripUserRepository.save(link);

        return new TripRegistrationResponse(link.getId(), trip.getId(), link.getRegistrationStatus().name());
    }

    private void validateRegistrationWindow(Trip trip) {
        LocalDate today = LocalDate.now();
        LocalDate open = trip.getRegistrationOpeningDate();
        LocalDate close = trip.getRegistrationClosingDate();
        if (open == null || close == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Période d'inscription non définie.");
        }
        if (today.isBefore(open) || today.isAfter(close)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Les inscriptions ne sont pas ouvertes.");
        }
    }
}