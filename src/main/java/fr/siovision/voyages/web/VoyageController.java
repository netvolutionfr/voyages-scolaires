package fr.siovision.voyages.web;

import fr.siovision.voyages.application.service.VoyageService;
import fr.siovision.voyages.domain.model.Voyage;
import fr.siovision.voyages.infrastructure.dto.VoyageDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/voyages")
public class VoyageController {
    @Autowired
    private VoyageService voyageService;

    @PostMapping
    public ResponseEntity<Voyage> createVoyage(@RequestBody VoyageDTO voyageDTO) {
        // Appeler le service pour créer un nouveau voyage
        Voyage saved = voyageService.createVoyage(voyageDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public ResponseEntity<Iterable<Voyage>> getAllVoyages() {
        // Récupérer tous les voyages
        Iterable<Voyage> voyages = voyageService.getAllVoyages();
        return ResponseEntity.ok(voyages);
    }
}
