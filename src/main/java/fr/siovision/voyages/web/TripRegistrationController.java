package fr.siovision.voyages.web;

import fr.siovision.voyages.application.service.TripRegistrationService;
import fr.siovision.voyages.infrastructure.dto.TripRegistrationRequest;
import fr.siovision.voyages.infrastructure.dto.TripRegistrationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trip/registrations")
@RequiredArgsConstructor
public class TripRegistrationController {

    private final TripRegistrationService registrationService;

    @PostMapping
    public ResponseEntity<TripRegistrationResponse> create(@RequestBody TripRegistrationRequest body) {
        TripRegistrationResponse res = registrationService.registerCurrentUserForTrip(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }
}