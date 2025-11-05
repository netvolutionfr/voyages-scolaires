package fr.siovision.voyages.web;

import fr.siovision.voyages.application.service.HealthFormAdminService;
import fr.siovision.voyages.infrastructure.dto.HealthFormAdminDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/api/users/{userPublicId}/health-form")
@RequiredArgsConstructor
public class HealthFormAdminController {

    private final HealthFormAdminService service;

    /**
     * Récupère la fiche sanitaire la plus récente d'un élève pour consultation par un enseignant/admin.
     * On passe tripId pour tracer la finalité de l’accès (RGPD).
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER') and @tripSecurity.canViewHealthForm(#userPublicId, #tripId)")
    public ResponseEntity<HealthFormAdminDTO> get(
            @PathVariable UUID userPublicId,
            @RequestParam Long tripId
    ) {
        HealthFormAdminDTO dto = service.get(userPublicId, tripId);

        // No-store pour éviter la mise en cache des données sensibles.
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().cachePrivate().getHeaderValue())
                .body(dto);
    }

    /**
     * Permet de savoir rapidement s'il existe une fiche sanitaire (sans exposer le contenu).
     */
    @RequestMapping(method = RequestMethod.HEAD)
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER') and @tripSecurity.canViewHealthForm(#userPublicId, #tripId)")
    public ResponseEntity<Void> head(
            @PathVariable UUID userPublicId,
            @RequestParam Long tripId
    ) {
        boolean exists = service.exists(userPublicId, tripId);
        return exists ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
}