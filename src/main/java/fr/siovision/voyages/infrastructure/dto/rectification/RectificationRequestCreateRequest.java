package fr.siovision.voyages.infrastructure.dto.rectification;

import lombok.Data;

@Data
public class RectificationRequestCreateRequest {
    private String field;
    private String requestedValue;
    private String reason;
}
