package fr.siovision.voyages.infrastructure.dto;

import fr.siovision.voyages.domain.model.FormalitePaysTemplate;
import fr.siovision.voyages.domain.model.TypeDocument;
import fr.siovision.voyages.domain.model.TypeFormalite;
import fr.siovision.voyages.domain.model.Voyage;
import fr.siovision.voyages.infrastructure.converter.StringSetCsvConverter;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FormaliteVoyageDTO {
    private Long id;
    private TypeDocumentDTO typeDocument;
    private TypeFormalite type; // (FILE ou FORM)
    private boolean required = true;
    private Integer delaiFournitureAvantDepart; // ex. 30
    private Set<String> acceptedMime = new LinkedHashSet<>(); // pour FILE
    private Integer maxSizeMb; // pour FILE
    private Integer delaiConservationApresVoyage; // politique de conservation de la formalité en jours
    private boolean storeScan = false; // mode vérif sans copie (false) vs scan conservé (true)
    private Map<String, Object> tripCondition;
    private String notes;
    private boolean manuallyAdded = false; // true si créé/édité manuellement après clonage
}
