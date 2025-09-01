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

    public ParticipantProfileResponse createParticipant(ParticipantRequest participantRequest) {
        // 1) AuthZ: only PARENT or ADMIN can create a participant
        User current = currentUserService.getCurrentUser();
        log.info("Current user is {} with roles {}", current.getEmail(), current.getRole());
        if (authorizationService.hasAnyRole(current, UserRole.PARENT, UserRole.ADMIN)) {
            throw new AccessDeniedException("Only PARENT or ADMIN can create a participant.");
        }

        // 2) Validate input
        if (participantRequest.getPrenom() == null || participantRequest.getPrenom().isBlank()
                || participantRequest.getNom() == null  || participantRequest.getNom().isBlank()
                || participantRequest.getDateNaissance() == null) {
            throw new IllegalArgumentException("prénom, nom, et date de naissance sont requis.");
        }
        if (participantRequest.isCreateStudentAccount() && (participantRequest.getEmail() == null || participantRequest.getEmail().isBlank())) {
            throw new IllegalArgumentException("L'Email est requis pour créer un compte étudiant.");
        }
        if (participantRequest.getDateNaissance().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La date de naissance ne peut pas être dans le futur.");
        }

        // 3) Build participant
        Participant p = new Participant();
        p.setPrenom(cap(participantRequest.getPrenom()));
        p.setNom(cap(participantRequest.getNom()));
        String newEmail = participantRequest.getEmail().toLowerCase().trim();
        if (!newEmail.isBlank()) {
            // Validate email format
            if (!newEmail.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}$")) {
                throw new IllegalArgumentException("L'email n'est pas au format valide.");
            }
        }
        p.setEmail(newEmail);
        LocalDate dateNaissance = participantRequest.getDateNaissance();
        p.setDateNaissance(dateNaissance);
        p.setSexe(participantRequest.getSexe() != null ? participantRequest.getSexe().toUpperCase() : null);
        p.setTelephone(participantRequest.getTelephone());

        // Legal guardian: if ADMIN and a specific guardian is provided, use it; else current user
        User guardian = resolveGuardian(current, participantRequest.getLegalGuardianId());
        log.info("Guardian {} is {}", guardian.getEmail(), guardian.getRole());
        if (guardian == null) {
            throw new IllegalArgumentException("Le parent légal doit être spécifié.");
        }
        p.setLegalGuardian(guardian);

        // Section: if provided, fetch it; else null
        Section section = resolveSection(participantRequest.getSectionId());
        log.info("Section ID provided: {}", participantRequest.getSectionId());
        if (section == null) {
            throw new IllegalArgumentException("La section doit être spécifiée.");
        }
        p.setSection(section);

        // 4) Persist early (gets UUID for FK if needed later)
        participantRepository.save(p);

        // 5) Optionally create a Keycloak STUDENT account and link it
        if (participantRequest.isCreateStudentAccount()) {
            // a) Create/ensure the Keycloak user (no password; UPDATE_PASSWORD email sent)
            UserRepresentation kc = keycloakService.createUser(participantRequest);

            // b) Upsert local User row with role STUDENT and KC ids
            User studentUser = userProvisioningService.upsertFromKeycloak(
                    kc.getId(),
                    kc.getEmail(),
                    kc.getLastName(),
                    kc.getFirstName(),
                    UserRole.STUDENT
            );

            // c) Enforce uniqueness: one student account ↔ one participant
            participantRepository.findByStudentAccountId(studentUser.getId()).ifPresent(existing -> {
                throw new IllegalStateException("This student account is already linked to another participant.");
            });

            // d) Link and save
            p.setStudentAccount(studentUser);
            participantRepository.save(p);
        }
        return getParticipantProfileResponse(p);
    }

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
