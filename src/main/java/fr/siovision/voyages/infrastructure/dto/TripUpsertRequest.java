package fr.siovision.voyages.infrastructure.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripUpsertRequest {
    private Long id; // null en création

    // Infos principales
    @NotBlank
    private String title;
    private String description;
    private String destination;

    /** Participation des familles (centimes d’€) */
    @PositiveOrZero
    private Integer familyContribution;

    /** Photo de couverture (URL/clé objet S3-MinIO) */
    private String coverPhotoUrl;

    /** Référence pays */
    @NotNull
    private Long countryId;

    /** Dates du voyage */
    @NotNull
    private DateRangeDTO tripDates;

    private boolean poll; // true si le voyage est en mode "sondage" (dates non fixées)

    @PositiveOrZero
    private Integer minParticipants;
    @PositiveOrZero
    private Integer maxParticipants;

    /** Période d’inscription (éventuellement null) */
    private DateRangeDTO registrationDates;

    /** Organisateurs (prof/admin) */
    @NotNull
    private List<UUID> chaperoneIds = new ArrayList<>();

    /** Sections concernées (peut être vide) */
    private List<UUID> sectionIds = new ArrayList<>();
}
