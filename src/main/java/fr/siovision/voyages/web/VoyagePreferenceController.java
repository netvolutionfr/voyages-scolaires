package fr.siovision.voyages.web;

import fr.siovision.voyages.application.service.VoyagePreferenceService;
import fr.siovision.voyages.infrastructure.dto.VoyagePreferencesRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/voyage-preferences")
public class VoyagePreferenceController {

    private final VoyagePreferenceService voyageprefService;

    public VoyagePreferenceController(VoyagePreferenceService voyageprefService) {
        this.voyageprefService = voyageprefService;
    }

    @PostMapping("/{voyageId}")
    public ResponseEntity<String> savePreference(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long voyageId,
            @RequestBody VoyagePreferencesRequest request) {
        if (voyageprefService.updateVoyagePref(jwt, voyageId, request.getInterest())) {
            return ResponseEntity.ok(request.getInterest());
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
