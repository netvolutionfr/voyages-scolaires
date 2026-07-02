package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.model.RectificationField;
import fr.siovision.voyages.domain.model.RectificationRequest;
import fr.siovision.voyages.domain.model.RectificationStatus;
import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.infrastructure.dto.rectification.RectificationRequestAdminDTO;
import fr.siovision.voyages.infrastructure.dto.rectification.RectificationRequestCreateRequest;
import fr.siovision.voyages.infrastructure.dto.rectification.RectificationRequestDTO;
import fr.siovision.voyages.infrastructure.dto.rectification.RectificationRequestResolveRequest;
import fr.siovision.voyages.infrastructure.repository.RectificationRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Droit de rectification (Art. 16 RGPD) pour les champs d'état civil : voir ADR-0002. */
@Service
@RequiredArgsConstructor
@Slf4j
public class RectificationRequestService {

    private final RectificationRequestRepository rectificationRequestRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public RectificationRequestDTO submit(RectificationRequestCreateRequest request) {
        User user = currentUserService.getCurrentUser();

        RectificationField field = parseField(request.getField());
        String value = request.getRequestedValue();
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("requestedValue is mandatory");
        }
        validateValueFormat(field, value);

        RectificationRequest entity = new RectificationRequest();
        entity.setUser(user);
        entity.setField(field);
        entity.setRequestedValue(value.trim());
        entity.setReason(request.getReason());
        entity.setStatus(RectificationStatus.PENDING);

        rectificationRequestRepository.save(entity);
        log.info("Demande de rectification ({}) soumise par l'utilisateur {}", field, user.getPublicId());

        return toDTO(entity);
    }

    @Transactional(readOnly = true)
    public Page<RectificationRequestAdminDTO> listByStatus(RectificationStatus status, Pageable pageable) {
        return rectificationRequestRepository.findByStatus(status, pageable).map(RectificationRequestService::toAdminDTO);
    }

    @Transactional
    public RectificationRequestDTO resolve(UUID requestId, RectificationRequestResolveRequest request) {
        RectificationRequest entity = rectificationRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Rectification request not found: " + requestId));

        RectificationStatus newStatus;
        try {
            newStatus = RectificationStatus.valueOf(request.getStatus());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid status: " + request.getStatus());
        }
        if (newStatus == RectificationStatus.PENDING) {
            throw new IllegalArgumentException("Cannot resolve a request back to PENDING");
        }

        User admin = currentUserService.getCurrentUser();
        entity.setStatus(newStatus);
        entity.setAdminComment(request.getAdminComment());
        entity.setProcessedBy(admin);
        entity.setProcessedAt(Instant.now());

        rectificationRequestRepository.save(entity);
        log.info("Demande de rectification {} résolue en {} par l'administrateur {}",
                entity.getId(), newStatus, admin.getPublicId());

        return toDTO(entity);
    }

    private static RectificationField parseField(String raw) {
        try {
            return RectificationField.valueOf(raw);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid field: " + raw);
        }
    }

    private static void validateValueFormat(RectificationField field, String value) {
        switch (field) {
            case EMAIL -> {
                if (!value.contains("@")) {
                    throw new IllegalArgumentException("Invalid email format: " + value);
                }
            }
            case BIRTH_DATE -> {
                try {
                    LocalDate.parse(value.trim());
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid birth date format: " + value);
                }
            }
            case FIRST_NAME, LAST_NAME -> {
                // non-blank déjà vérifié par l'appelant
            }
        }
    }

    private static RectificationRequestDTO toDTO(RectificationRequest entity) {
        return new RectificationRequestDTO(
                entity.getId(),
                entity.getField().name(),
                entity.getRequestedValue(),
                entity.getReason(),
                entity.getStatus().name(),
                entity.getCreatedAt(),
                entity.getProcessedAt(),
                entity.getAdminComment()
        );
    }

    private static RectificationRequestAdminDTO toAdminDTO(RectificationRequest entity) {
        User user = entity.getUser();
        String fullName = ((user.getFirstName() != null ? user.getFirstName() : "")
                + " " + (user.getLastName() != null ? user.getLastName() : "")).trim();

        return new RectificationRequestAdminDTO(
                entity.getId(),
                user.getPublicId(),
                fullName,
                user.getEmail(),
                entity.getField().name(),
                entity.getRequestedValue(),
                entity.getReason(),
                entity.getStatus().name(),
                entity.getCreatedAt()
        );
    }
}
