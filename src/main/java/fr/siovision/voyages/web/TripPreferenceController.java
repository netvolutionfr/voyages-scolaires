package fr.siovision.voyages.web;

import fr.siovision.voyages.application.service.TripPreferenceService;
import fr.siovision.voyages.infrastructure.dto.TripPreferenceRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trip-preferences")
public class TripPreferenceController {

    private final TripPreferenceService tripPrefService;

    public TripPreferenceController(TripPreferenceService tripPrefService) {
        this.tripPrefService = tripPrefService;
    }

    @PostMapping("/{voyageId}")
    public ResponseEntity<String> savePreference(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long voyageId,
            @RequestBody TripPreferenceRequest request) {
        if (tripPrefService.updateVoyagePref(jwt, voyageId, request.getInterest())) {
            return ResponseEntity.ok(request.getInterest());
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
