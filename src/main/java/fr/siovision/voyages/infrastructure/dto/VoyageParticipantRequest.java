package fr.siovision.voyages.infrastructure.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class VoyageParticipantRequest {
    private UUID participantId;
    private Boolean accompagnateur;
    private Boolean organisateur;
}
