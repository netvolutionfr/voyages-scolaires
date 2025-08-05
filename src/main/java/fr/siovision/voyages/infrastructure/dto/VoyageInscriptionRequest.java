package fr.siovision.voyages.infrastructure.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoyageInscriptionRequest {
    private String messageMotivation;
    private Boolean jeMEngage;
}
