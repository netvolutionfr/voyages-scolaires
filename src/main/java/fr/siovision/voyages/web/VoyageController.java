package fr.siovision.voyages.web;

import fr.siovision.voyages.application.service.VoyageService;
import fr.siovision.voyages.domain.model.Voyage;
import fr.siovision.voyages.infrastructure.dto.VoyageInscriptionRequest;
import fr.siovision.voyages.infrastructure.dto.VoyageRequest;
import fr.siovision.voyages.infrastructure.dto.VoyageParticipantRequest;
import fr.siovision.voyages.infrastructure.dto.VoyagesOuvertsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/voyages")
public class VoyageController {
    @Autowired
    private VoyageService voyageService;

    @PostMapping
    @PreAuthorize("hasAnyRole('admin', 'manager')")
    public ResponseEntity<Voyage> createVoyage(@RequestBody VoyageRequest voyageRequest) {
        // Appeler le service pour créer un nouveau voyage
        Voyage saved = voyageService.createVoyage(voyageRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('admin', 'manager')")
    public ResponseEntity<Iterable<Voyage>> getAllVoyages() {
        // Récupérer tous les voyages
        Iterable<Voyage> voyages = voyageService.getAllVoyages();
        return ResponseEntity.ok(voyages);
    }

    @PostMapping("/{voyageId}/participants")
    @PreAuthorize("hasAnyRole('admin', 'manager')")
    public ResponseEntity<Void> addParticipantToVoyage(
            @PathVariable Long voyageId,
            @RequestBody VoyageParticipantRequest request) {
        // Ajouter un participant à un voyage
        voyageService.addParticipantToVoyage(voyageId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/ouverts")
    @PreAuthorize("hasAnyRole('admin', 'manager', 'user')")
    public ResponseEntity<Iterable<VoyagesOuvertsResponse>> getVoyagesOuverts(Authentication authentication) {
        // Récupérer les voyages ouverts
        Iterable<VoyagesOuvertsResponse> voyages = voyageService.getVoyagesOuverts();
        return ResponseEntity.ok(voyages);
    }

    @PostMapping("/{id}/inscription")
    @PreAuthorize("hasRole('user')")
    public ResponseEntity<Void> inscrire(
            @PathVariable Long id,
            @RequestBody VoyageInscriptionRequest request,
            @AuthenticationPrincipal Jwt principal) {

        String email = principal.getClaimAsString("email");
        voyageService.inscrireParticipant(id, email, request);

        return ResponseEntity.ok().build();
    }
}
