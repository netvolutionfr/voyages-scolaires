package fr.siovision.voyages.infrastructure.dto;

import fr.siovision.voyages.domain.model.FormalityType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TripFormalityDTO {
    private Long id;
    private DocumentTypeDTO documentType;
    private FormalityType type; // (FILE ou FORM)
    private boolean required = true;
    private Integer daysBeforeTrip; // délai avant le départ pour fournir la formalité
    private Set<String> acceptedMime = new LinkedHashSet<>(); // pour FILE
    private Integer maxSizeMb; // pour FILE
    private Integer retentionDays; // pour FILE
    private boolean storeScan = false; // mode vérif sans copie (false) vs scan conservé (true)
    private Map<String, Object> tripCondition;
    private String notes;
    private boolean manuallyAdded = false; // true si créé/édité manuellement après clonage
}
