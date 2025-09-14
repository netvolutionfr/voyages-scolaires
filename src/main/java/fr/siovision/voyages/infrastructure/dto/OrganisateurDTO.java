package fr.siovision.voyages.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrganisateurDTO {
    private UUID publicId;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
}
