package fr.siovision.voyages.infrastructure.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class ParticipantRequest {
    private String nom;
    private String prenom;
    private String sexe; // M, F, Autre
    private String email;
    private String telephone;
    private LocalDate dateNaissance;
    private Long sectionId; // ID de la section à laquelle le participant est associé
    private UUID legalGuardianId; // ID du parent légal primaire (si mineur)
    private UUID studentAccountId; // ID du compte de l'élève (optionnel : mineur avec accès; obligatoire si majeur autonome)
    private boolean createStudentAccount; // Indique si un compte élève doit être créé
}
