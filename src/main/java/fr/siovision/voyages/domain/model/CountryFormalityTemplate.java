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
@Table(name = "country_formality_templates",
        indexes = {
                @Index(name = "idx_fpt_country", columnList = "country_id"),
                @Index(name = "idx_fpt_doc_type", columnList = "document_type_id")
        })
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class CountryFormalityTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cust_seq")
    @SequenceGenerator(name = "cust_seq", sequenceName = "cust_seq")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id", referencedColumnName = "id")
    private Country country;


    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "document_type_id")
    private DocumentType documentType;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FormalityType type; // (FILE ou FORM)


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
    private Integer daysRetentionAfterTrip;

    @Column(name = "store_scan")
    private Boolean storeScan = Boolean.TRUE; // pour FILE, si on stocke le scan dans notre système


    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> tripCondition;


    @Column(length = 512)
    private String notes;

    public Boolean getStoreScan() {
        return storeScan != null ? storeScan : Boolean.TRUE;
    }
}
