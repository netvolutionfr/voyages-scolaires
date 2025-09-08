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
@Table(name = "formalites_voyage",
        indexes = {
                @Index(name = "idx_fv_voyage", columnList = "voyage_id"),
                @Index(name = "idx_fv_doc_type", columnList = "type_document_id")
        })
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class FormaliteVoyage {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cust_seq")
    @SequenceGenerator(name = "cust_seq", sequenceName = "cust_seq")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "voyage_id", referencedColumnName = "id")
    private Voyage voyage;

    // lien optionnel vers le template d’origine (null si ajout manuel)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_template_id")
    private FormalitePaysTemplate sourceTemplate;


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
    private Boolean storeScan = Boolean.TRUE; // pour FILE, si on stocke le scan dans notre système


    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> tripCondition;

    @Column(length = 512)
    private String notes;


    @Column(name = "manually_added")
    private boolean manuallyAdded = false; // true si créé/édité manuellement après clonage

    public Boolean getStoreScan() {
        return storeScan != null ? storeScan : Boolean.TRUE;
    }

}
