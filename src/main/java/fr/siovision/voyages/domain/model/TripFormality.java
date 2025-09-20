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
@Table(name = "trip_formality",
        indexes = {
                @Index(name = "idx_fv_trip", columnList = "trip_id"),
                @Index(name = "idx_fv_doc_type", columnList = "document_type_id"),
        })
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class TripFormality {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cust_seq")
    @SequenceGenerator(name = "cust_seq", sequenceName = "cust_seq")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", referencedColumnName = "id")
    private Trip trip;

    // lien optionnel vers le template d’origine (null si ajout manuel)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_template_id")
    private CountryFormalityTemplate sourceTemplate;


    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "document_type_id")
    private DocumentType documentType;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FormalityType formalityType; // (FILE ou FORM)


    @Column(nullable = false)
    private boolean required = true;


    @Column(name = "days_before_trip")
    private Integer daysBeforeTrip; // ex. 30

    @Convert(converter = StringSetCsvConverter.class)
    @Column(name = "accepted_mime", length = 1024)
    private Set<String> acceptedMime = new LinkedHashSet<>(); // pour FILE


    @Column(name = "max_size_mb")
    private Integer maxSizeMb; // pour FILE


    @Column(name = "days_retention_after_trip")
    private Integer daysRetentionAfterTrip; // politique de conservation de la formalité en jours


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
