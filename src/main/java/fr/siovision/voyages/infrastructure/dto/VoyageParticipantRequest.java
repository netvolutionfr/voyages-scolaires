package fr.siovision.voyages.infrastructure.dto;

import lombok.Data;

@Data
public class VoyageParticipantRequest {
    private Long participantId;
    private Boolean accompagnateur;
    private Boolean organisateur;
}
