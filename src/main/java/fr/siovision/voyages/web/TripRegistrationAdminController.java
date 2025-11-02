package fr.siovision.voyages.web;

import fr.siovision.voyages.application.service.TripRegistrationAdminService;
import fr.siovision.voyages.infrastructure.dto.RegistrationAdminUpdateRequest;
import fr.siovision.voyages.infrastructure.dto.RegistrationAdminUpdateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/registrations")
@RequiredArgsConstructor
public class TripRegistrationAdminController {

    private final TripRegistrationAdminService service;

    @PatchMapping("/{tripUserId}")
    public RegistrationAdminUpdateResponse update(
            @PathVariable Long tripUserId,
            @RequestBody RegistrationAdminUpdateRequest body
    ) {
        return service.updateStatus(tripUserId, body);
    }
}