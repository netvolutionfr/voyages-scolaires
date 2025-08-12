package fr.siovision.voyages.web;


import fr.siovision.voyages.application.service.KeycloakService;
import fr.siovision.voyages.application.service.ParticipantService;
import fr.siovision.voyages.domain.model.Participant;
import fr.siovision.voyages.infrastructure.dto.ParticipantProfileResponse;
import fr.siovision.voyages.infrastructure.dto.ParticipantRequest;
import fr.siovision.voyages.infrastructure.dto.SectionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
public class ParticipantController {
    @Autowired
    private ParticipantService participantService;
    @Autowired
    private KeycloakService keycloakService;

    @GetMapping("/api/participants")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT')")
    public ResponseEntity<Page<ParticipantProfileResponse>> getParticipants(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ParticipantProfileResponse> participants = participantService.getAllParticipants(q, pageable);
        return ResponseEntity.ok(participants);
    }

    @PostMapping("/api/participants")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT')")
    public ResponseEntity<String> createParticipant(@RequestBody ParticipantRequest participantRequest) {
        // Appeler le service pour créer un nouveau participant
        log.info("Creating participant in the application for email {}", participantRequest.getEmail());
        ParticipantProfileResponse savedParticipant = participantService.createParticipant(participantRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedParticipant.getId().toString());
    }

    @GetMapping("/api/participants/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT')")
    public ResponseEntity<ParticipantProfileResponse> getParticipantById(@PathVariable UUID id) {
        // Récupérer un participant par son ID
        ParticipantProfileResponse participant = participantService.getParticipantById(id);
        if (participant != null) {
            return ResponseEntity.ok(participant);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
