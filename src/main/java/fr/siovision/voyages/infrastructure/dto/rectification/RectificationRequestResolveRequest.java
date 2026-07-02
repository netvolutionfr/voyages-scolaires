package fr.siovision.voyages.infrastructure.dto.rectification;

import lombok.Data;

@Data
public class RectificationRequestResolveRequest {
    private String status;
    private String adminComment;
}
