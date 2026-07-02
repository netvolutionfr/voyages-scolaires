package fr.siovision.voyages.web;

import fr.siovision.voyages.application.service.RectificationRequestService;
import fr.siovision.voyages.domain.model.RectificationStatus;
import fr.siovision.voyages.infrastructure.dto.rectification.RectificationRequestAdminDTO;
import fr.siovision.voyages.infrastructure.dto.rectification.RectificationRequestCreateRequest;
import fr.siovision.voyages.infrastructure.dto.rectification.RectificationRequestDTO;
import fr.siovision.voyages.infrastructure.dto.rectification.RectificationRequestResolveRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RectificationRequestController {

    private final RectificationRequestService rectificationRequestService;

    @PostMapping("/me/rectification-request")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RectificationRequestDTO> submit(@RequestBody RectificationRequestCreateRequest request) {
        RectificationRequestDTO created = rectificationRequestService.submit(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/users/rectification-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<RectificationRequestAdminDTO>> list(
            @RequestParam(defaultValue = "PENDING") RectificationStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(rectificationRequestService.listByStatus(status, pageable));
    }

    @PatchMapping("/users/rectification-requests/{requestId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RectificationRequestDTO> resolve(
            @PathVariable UUID requestId,
            @RequestBody RectificationRequestResolveRequest request
    ) {
        return ResponseEntity.ok(rectificationRequestService.resolve(requestId, request));
    }
}
