package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.model.Participant;
import fr.siovision.voyages.domain.model.Section;
import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.domain.model.UserRole;
import fr.siovision.voyages.infrastructure.dto.ParticipantProfileResponse;
import fr.siovision.voyages.infrastructure.dto.SectionDTO;
import fr.siovision.voyages.infrastructure.mapper.SectionMapper;
import fr.siovision.voyages.infrastructure.repository.ParticipantRepository;
import fr.siovision.voyages.infrastructure.repository.SectionRepository;
import fr.siovision.voyages.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

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
    private final SectionMapper sectionMapper;

    public Page<SectionDTO> list(String q, Pageable pageable) {
        Page<Section> sections = sectionRepository.search(q, pageable);
        return sections.map(sectionMapper::toDTO);
    }

    public Page<ParticipantProfileResponse> getAllParticipants(String q, Pageable pageable) {
        Page<Participant> participants = participantRepository.search(q, pageable);
        User currentUser = currentUserService.getCurrentUser();
        if (authorizationService.hasAnyRole(currentUser, UserRole.ADMIN, UserRole.PARENT)) {
            throw new AccessDeniedException("Only ADMIN or PARENT can view participants.");
        }

        return participants.map(ParticipantService::getParticipantProfileResponse);
    }

    private static ParticipantProfileResponse getParticipantProfileResponse(Participant participant) {
        return new ParticipantProfileResponse(participant);
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
