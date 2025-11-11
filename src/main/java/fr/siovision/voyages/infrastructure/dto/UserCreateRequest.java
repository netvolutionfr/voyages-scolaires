package fr.siovision.voyages.infrastructure.dto;

import fr.siovision.voyages.domain.model.UserRole;
import lombok.Data;

import java.util.UUID;

@Data
public class UserCreateRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String gender;
    private String telephone;
    private String birthDate;
    private UUID sectionPublicId;
    private UserRole role;
}
