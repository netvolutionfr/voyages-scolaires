package fr.siovision.voyages.infrastructure.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripRegistrationRequest {
    private String motivationMessage;
    private Boolean hasCommitted;
}
