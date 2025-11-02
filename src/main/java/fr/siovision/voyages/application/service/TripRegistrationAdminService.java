package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.model.TripUser;
import fr.siovision.voyages.infrastructure.dto.RegistrationAdminUpdateRequest;
import fr.siovision.voyages.infrastructure.dto.RegistrationAdminUpdateResponse;
import fr.siovision.voyages.infrastructure.repository.TripUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TripRegistrationAdminService {
    private final TripUserRepository tripUserRepository;

    @Transactional
    public RegistrationAdminUpdateResponse updateStatus(Long tripUserId, RegistrationAdminUpdateRequest req) {
        TripUser tu = tripUserRepository.findById(tripUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inscription inconnue"));

        // Ici tu peux ajouter un contrôle: "l'appelant est-il organisateur de ce trip ?"
        // (ex: security util: assertOrganizer(tu.getTrip()))

        tu.setRegistrationStatus(req.status());
        tu.setDecisionDate(LocalDateTime.now());
        tu.setDecisionMessage(req.adminMessage());

        // Option: si REJECTED/CANCELLED, tu peux aussi nettoyer des ressources associées
        // (ex: mettre l'élève hors "participants confirmés" pour les comptes/quotas)

        tripUserRepository.save(tu);

        return new RegistrationAdminUpdateResponse(
                tu.getId(),
                tu.getTrip().getId(),
                tu.getUser().getId(),
                tu.getRegistrationStatus().name()
        );
    }
}