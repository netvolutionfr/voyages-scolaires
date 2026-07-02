package fr.siovision.voyages.infrastructure.dto;

import lombok.Data;

/** PATCH /api/me/profile — champs de contact modifiables en self-service. Absent = inchangé. */
@Data
public class ProfileUpdateRequest {
    private String telephone;
    private String displayName;
    private String gender;
}
