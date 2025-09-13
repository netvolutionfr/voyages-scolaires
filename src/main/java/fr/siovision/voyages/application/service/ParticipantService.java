package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.model.Participant;
import fr.siovision.voyages.domain.model.Section;
import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.domain.model.UserRole;
import fr.siovision.voyages.infrastructure.dto.ParticipantProfileResponse;
import fr.siovision.voyages.infrastructure.dto.ParticipantRequest;
import fr.siovision.voyages.infrastructure.dto.SectionDTO;
import fr.siovision.voyages.infrastructure.repository.ParticipantRepository;
import fr.siovision.voyages.infrastructure.repository.SectionRepository;
import fr.siovision.voyages.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipantService {
    private final ParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final AuthorizationService authorizationService;
    private final SectionRepository sectionRepository;
    private final KeycloakService keycloakService;
    private final UserProvisioningService userProvisioningService;

    public Page<SectionDTO> list(String q, Pageable pageable) {
        Page<Section> sections = sectionRepository.search(q, pageable);
        return sections.map(section -> new SectionDTO(section.getId(), section.getLibelle(), section.getDescription()));
    }

    public Page<ParticipantProfileResponse> getAllParticipants(String q, Pageable pageable) {
        Page<Participant> participants = participantRepository.search(q, pageable);
        User currentUser = currentUserService.getCurrentUser();
        if (authorizationService.hasAnyRole(currentUser, UserRole.ADMIN, UserRole.PARENT)) {
            throw new AccessDeniedException("Only ADMIN or PARENT can view participants.");
        }

        // Check if the current user is a guardian of any participant
//        boolean isGuardian = authorizationService.hasRole(currentUser, UserRole.PARENT);
//        return participants.stream()
////                .filter(participant -> !isGuardian || participant.getLegalGuardian().getId().equals(currentUser.getId()))
//                .map(ParticipantService::getParticipantProfileResponse)
//                .collect(Page.toPageable(participants.getPageable(), participants.getTotalElements()));
        return participants.map(ParticipantService::getParticipantProfileResponse);
    }

    public Optional<Participant> getParticipantByEmail(String email) {
        // Vérifier si un participant avec l'email donné existe
        return participantRepository.findAll().stream()
                .filter(participant -> participant.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }

    public ParticipantProfileResponse getParticipant(String email) {
        Participant participant = participantRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Participant not found with email: " + email));

        User currentUser = currentUserService.getCurrentUser();
        if (authorizationService.hasAnyRole(currentUser, UserRole.ADMIN, UserRole.PARENT)) {
            throw new AccessDeniedException("Only ADMIN or PARENT can view participant profiles.");
        }
        
        // Check if the current user is the guardian of the participant
        if (authorizationService.hasRole(currentUser, UserRole.PARENT) && !participant.getLegalGuardian().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You are not authorized to view this participant's profile.");
        }

        // Build the response

        return getParticipantProfileResponse(participant);
    }

    private static ParticipantProfileResponse getParticipantProfileResponse(Participant participant) {
        return new ParticipantProfileResponse(participant);
    }

    private static String cap(String s) {
        if (s == null || s.isBlank()) return s;
        return s.substring(0,1).toUpperCase() + s.substring(1).trim();
    }

    private User resolveGuardian(User current, UUID optionalGuardianId) {
        if (authorizationService.hasRole(current, UserRole.ADMIN) && optionalGuardianId != null) {
            return userRepository.findById(optionalGuardianId)
                    .orElseThrow(() -> new IllegalArgumentException("Guardian user not found: " + optionalGuardianId));
        }
        return current; // default: the current PARENT (or ADMIN acting as parent)
    }

    private Section resolveSection(Long sectionId) {
        if (sectionId == null) return null; // no section provided
        return sectionRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section not found: " + sectionId));
    }

    public ParticipantProfileResponse getParticipantById(UUID id) {
        // Récupérer un participant par son ID
        Participant participant = participantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Participant not found with id: " + id));

        User currentUser = currentUserService.getCurrentUser();
        if (authorizationService.hasAnyRole(currentUser, UserRole.ADMIN, UserRole.PARENT)) {
            throw new AccessDeniedException("Only ADMIN or PARENT can view participant profiles.");
        }

        // Check if the current user is the guardian of the participant
        if (authorizationService.hasRole(currentUser, UserRole.PARENT) && !participant.getLegalGuardian().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You are not authorized to view this participant's profile.");
        }

        return getParticipantProfileResponse(participant);
    }
}
