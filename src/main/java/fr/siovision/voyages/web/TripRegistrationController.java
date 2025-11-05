package fr.siovision.voyages.web;

import fr.siovision.voyages.application.service.TripRegistrationAdminService;
import fr.siovision.voyages.application.service.TripRegistrationService;
import fr.siovision.voyages.infrastructure.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trips/registrations")
@RequiredArgsConstructor
public class TripRegistrationController {

    private final TripRegistrationService registrationService;

    @PostMapping
    public ResponseEntity<TripRegistrationResponse> create(@RequestBody TripRegistrationRequest body) {
        TripRegistrationResponse res = registrationService.registerCurrentUserForTrip(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    private final TripRegistrationAdminService service;

    @PatchMapping("/{tripUserId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER') and @tripSecurity.canViewTrip(#tripId)")
    public RegistrationAdminUpdateResponse update(
            @RequestParam Long tripId,
            @PathVariable Long tripUserId,
            @RequestBody RegistrationAdminUpdateRequest body
    ) {
        return service.updateStatus(tripUserId, body);
    }

    @GetMapping()
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER') and @tripSecurity.canViewTrip(#tripId)")
    public Page<TripRegistrationAdminViewDTO> list(
            @RequestParam Long tripId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(defaultValue = "false") boolean includeDocSummary,
            Pageable pageable
    ) {
        return service.listTripRegistrations(tripId, status, q, sectionId, includeDocSummary, pageable);
    }

    @GetMapping("/{registrationId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER') and @tripSecurity.canViewRegistration(#registrationId)")
    public RegistrationAdminDetailDTO get(@PathVariable Long registrationId) {
        return service.getRegistrationDetail(registrationId);
    }

}