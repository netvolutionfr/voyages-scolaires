package fr.siovision.voyages.domain.model;


import fr.siovision.voyages.infrastructure.converter.StringSetCsvConverter;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "formalites_pays_templates",
        indexes = {
                @Index(name = "idx_fpt_country", columnList = "pays_id"),
                @Index(name = "idx_fpt_doc_type", columnList = "type_document_id")
        })
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class FormalitePaysTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "pays_id", referencedColumnName = "id")
    private Pays pays;


    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "type_document_id")
    private TypeDocument typeDocument;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TypeFormalite type; // (FILE ou FORM)


    @Column(nullable = false)
    private boolean required = true;


    @Column(name = "delai_fourniture_avant_depart")
    private Integer delaiFournitureAvantDepart; // ex. 30


    @Convert(converter = StringSetCsvConverter.class)
    @Column(name = "accepted_mime", length = 1024)
    private Set<String> acceptedMime = new LinkedHashSet<>(); // pour FILE


    @Column(name = "max_size_mb")
    private Integer maxSizeMb; // pour FILE


    @Column(name = "delai_conservation_apres_voyage")
    private Integer delaiConservationApresVoyage; // politique de conservation de la formalité en jours


    @Column(name = "store_scan")
    private Boolean storeScan = Boolean.FALSE; // mode vérif sans copie (false) vs scan conservé (true)


    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> tripCondition;


    @Column(length = 512)
    private String notes;
}
