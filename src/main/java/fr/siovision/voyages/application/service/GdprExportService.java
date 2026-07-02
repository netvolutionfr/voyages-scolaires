package fr.siovision.voyages.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.siovision.voyages.domain.model.ParentChild;
import fr.siovision.voyages.domain.model.StudentHealthForm;
import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.infrastructure.dto.gdpr.*;
import fr.siovision.voyages.infrastructure.repository.DocumentRepository;
import fr.siovision.voyages.infrastructure.repository.ParentChildRepository;
import fr.siovision.voyages.infrastructure.repository.RefreshTokenRepository;
import fr.siovision.voyages.infrastructure.repository.StudentHealthFormRepository;
import fr.siovision.voyages.infrastructure.repository.WebAuthnCredentialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Droit d'accès (Art. 15 RGPD) et portabilité (Art. 20 RGPD) : export JSON des données de l'utilisateur courant. */
@Service
@RequiredArgsConstructor
@Slf4j
public class GdprExportService {

    private static final String FORMAT = "voyages-gdpr-export/v1";

    private final CurrentUserService currentUserService;
    private final DocumentRepository documentRepository;
    private final StudentHealthFormRepository studentHealthFormRepository;
    private final HealthFormPayloadRenderer healthFormPayloadRenderer;
    private final ParentChildRepository parentChildRepository;
    private final WebAuthnCredentialRepository webAuthnCredentialRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public GdprExportResponse export() {
        User user = currentUserService.getCurrentUser();
        log.info("Export RGPD généré pour l'utilisateur {}", user.getPublicId());

        return new GdprExportResponse(
                Instant.now(),
                FORMAT,
                buildProfile(user),
                buildConsent(user),
                buildLegalGuardian(user),
                buildFamilyLinks(user),
                buildTrips(user),
                buildDocuments(user),
                buildHealthForm(user),
                buildSecurity(user)
        );
    }

    private GdprProfileDTO buildProfile(User user) {
        return new GdprProfileDTO(
                user.getPublicId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getDisplayName(),
                user.getGender(),
                user.getBirthDate() != null ? user.getBirthDate().toString() : null,
                user.getTelephone(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getStatus() != null ? user.getStatus().name() : null,
                user.getSection() != null ? user.getSection().getLabel() : null,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private GdprConsentDTO buildConsent(User user) {
        return new GdprConsentDTO(user.getConsentGivenAt(), user.getConsentText());
    }

    private GdprLegalGuardianDTO buildLegalGuardian(User user) {
        User guardian = user.getLegalGuardian();
        if (guardian == null) {
            return null;
        }
        return new GdprLegalGuardianDTO(guardian.getPublicId(), fullName(guardian), guardian.getEmail());
    }

    private List<GdprFamilyLinkDTO> buildFamilyLinks(User user) {
        List<GdprFamilyLinkDTO> links = new ArrayList<>();
        for (ParentChild link : parentChildRepository.findByParentId(user.getId())) {
            User child = link.getChild();
            links.add(new GdprFamilyLinkDTO("CHILD", child.getPublicId(), fullName(child), child.getEmail()));
        }
        for (ParentChild link : parentChildRepository.findByChildId(user.getId())) {
            User parent = link.getParent();
            links.add(new GdprFamilyLinkDTO("PARENT", parent.getPublicId(), fullName(parent), parent.getEmail()));
        }
        return links;
    }

    private GdprTripsDTO buildTrips(User user) {
        List<GdprTripRegistrationDTO> registrations = user.getTrips().stream()
                .map(tu -> new GdprTripRegistrationDTO(
                        tu.getTrip().getTitle(),
                        tu.getTrip().getDestination(),
                        tu.getTrip().getDepartureDate(),
                        tu.getTrip().getReturnDate(),
                        tu.getRegistrationStatus() != null ? tu.getRegistrationStatus().name() : null,
                        tu.getRegistrationDate(),
                        tu.getDecisionDate(),
                        tu.getDecisionMessage()
                ))
                .toList();

        List<GdprTripPreferenceDTO> preferences = user.getTripPreferences().stream()
                .map(tp -> new GdprTripPreferenceDTO(tp.getTrip().getTitle(), tp.getInterest()))
                .toList();

        return new GdprTripsDTO(registrations, preferences);
    }

    private List<GdprDocumentDTO> buildDocuments(User user) {
        return documentRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(d -> new GdprDocumentDTO(
                        d.getDocumentType() != null ? d.getDocumentType().getLabel() : null,
                        d.getOriginalFilename(),
                        d.getMime(),
                        d.getSize(),
                        d.getSha256(),
                        d.getFileNumber(),
                        d.getDeliveryDate(),
                        d.getExpirationDate(),
                        d.getCreatedAt()
                ))
                .toList();
    }

    private GdprHealthFormDTO buildHealthForm(User user) {
        StudentHealthForm form = studentHealthFormRepository.findByStudentId(user.getId());
        if (form == null) {
            return null;
        }

        JsonNode payload = null;
        String rendered = healthFormPayloadRenderer.render(form);
        if (rendered != null) {
            try {
                payload = objectMapper.readTree(rendered);
            } catch (Exception e) {
                log.warn("Fiche santé illisible lors de l'export RGPD de l'utilisateur {}", user.getPublicId(), e);
            }
        }

        return new GdprHealthFormDTO(form.getSignedAt(), form.getValidUntil(), payload);
    }

    private GdprSecurityDTO buildSecurity(User user) {
        List<GdprPasskeyDTO> passkeys = webAuthnCredentialRepository.findAllByUserId(user.getId()).stream()
                .map(c -> new GdprPasskeyDTO(c.getCreatedAt(), c.getAaguid()))
                .toList();

        Instant lastLoginAt = refreshTokenRepository.findMaxLastUsedAtByUser(user).orElse(null);

        return new GdprSecurityDTO(passkeys, lastLoginAt);
    }

    private static String fullName(User user) {
        String first = user.getFirstName() != null ? user.getFirstName() : "";
        String last = user.getLastName() != null ? user.getLastName() : "";
        return (first + " " + last).trim();
    }
}
