package fr.siovision.voyages.infrastructure.dto;

import fr.siovision.voyages.domain.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class UserCreateRequest {
    @NotBlank private String firstName;
    @NotBlank private String lastName;
    @NotBlank @Email private String email;
    private String gender;
    private String telephone;
    private String birthDate;
    @NotNull private UUID sectionPublicId;
    @NotNull private UserRole role;
}
