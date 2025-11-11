package fr.siovision.voyages.infrastructure.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class UserUpdateRequest {
    private String firstName;
    private String lastName;
    private String gender;
    private String telephone;
    private String birthDate;
    private UUID sectionPublicId;
}
