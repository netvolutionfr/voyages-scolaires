package fr.siovision.voyages.infrastructure.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class TripParticipantRequest {
    private UUID participantId;
    private Boolean chaperone;
}
