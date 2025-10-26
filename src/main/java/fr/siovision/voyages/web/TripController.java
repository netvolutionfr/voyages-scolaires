package fr.siovision.voyages.web;

import fr.siovision.voyages.application.service.TripService;
import fr.siovision.voyages.domain.model.Trip;
import fr.siovision.voyages.infrastructure.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trips")
public class TripController {
    @Autowired
    private TripService tripService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<String> createTrip(@RequestBody TripUpsertRequest tripRequest) {
        // Appeler le service pour créer un nouveau voyage
        Trip saved = tripService.createTrip(tripRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.getId().toString());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<String> updateTrip(@PathVariable Long id, @RequestBody TripUpsertRequest tripRequest) {
        // Mettre à jour une section existante
        Trip updatedVoyage = tripService.updateTrip(id, tripRequest);
        return ResponseEntity.status(HttpStatus.OK).body(updatedVoyage.getId().toString());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    public ResponseEntity<Page<TripDetailDTO>> list(
            @PageableDefault(size = 20, sort = "departureDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<TripDetailDTO> trips = tripService.list(pageable);
        return ResponseEntity.ok(trips);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    @RequestMapping("/{id}")
    public ResponseEntity<TripDetailDTO> getTripById(@PathVariable Long id) {
        TripDetailDTO trip = tripService.getTripById(id);
        return ResponseEntity.ok(trip);
    }
}
