package fr.siovision.voyages.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private UUID publicId;
    private String email;
    private String nom;
    private String prenom;
    private String telephone;
    private Boolean validated;
    private String role; // ADMIN, PARTICIPANT, ORGANISATEUR
}
