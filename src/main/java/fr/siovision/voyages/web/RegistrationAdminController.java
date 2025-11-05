package fr.siovision.voyages.web;

import fr.siovision.voyages.application.service.TripRegistrationAdminService;
import fr.siovision.voyages.infrastructure.dto.RegistrationAdminDetailDTO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/registrations")
public class RegistrationAdminController {

    private final TripRegistrationAdminService service;

    public RegistrationAdminController(TripRegistrationAdminService service) {
        this.service = service;
    }


}