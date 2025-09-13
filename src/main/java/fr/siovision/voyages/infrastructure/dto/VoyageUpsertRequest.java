package fr.siovision.voyages.infrastructure.dto;

import fr.siovision.voyages.domain.model.Secteur;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoyageUpsertRequest {
    private Long id; // null en création

    // Infos principales
    @NotBlank
    private String nom;
    private String description;
    private String destination;

    /** Prix total du voyage (centimes d’€) */
    @PositiveOrZero
    private Integer prixTotal;

    /** Participation des familles (centimes d’€) */
    @PositiveOrZero
    private Integer participationDesFamilles;

    /** Photo de couverture (URL/clé objet S3-MinIO) */
    private String coverPhotoUrl;

    /** Référence pays */
    @NotNull
    private Long paysId;

    /** Dates du voyage */
    @NotNull
    private DateRangeDTO datesVoyage;

    @PositiveOrZero
    private Integer nombreMinParticipants;
    @PositiveOrZero
    private Integer nombreMaxParticipants;

    /** Période d’inscription (éventuellement null) */
    private DateRangeDTO datesInscription;

    /** Organisateurs (prof/admin) */
    @NotNull
    private List<Long> organisateurIds = new ArrayList<>();

    /** Sections concernées (peut être vide) */
    private List<Long> sectionIds = new ArrayList<>();

    /** Secteurs (peut être vide) */
    private Set<Secteur> secteurs = new HashSet<>();
}
