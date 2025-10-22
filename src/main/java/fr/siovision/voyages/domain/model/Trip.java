package fr.siovision.voyages.domain.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Formula;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;


@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cust_seq")
    @SequenceGenerator(name = "cust_seq", sequenceName = "cust_seq")
    private Long id;

    @EqualsAndHashCode.Include
    @Column(nullable = false, updatable = false, unique = true)
    private UUID publicId = UUID.randomUUID();

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String destination;

    private Integer totalPrice; // en centimes d'euros
    private Integer familyContribution; // en centimes d'euros
    @ManyToOne(fetch = FetchType.LAZY)
    private Country country;

    private LocalDate departureDate;
    private LocalDate returnDate;
    private Integer minParticipants;
    private Integer maxParticipants;
    private LocalDate registrationOpeningDate;
    private LocalDate registrationClosingDate;
    private Boolean poll; // true si le voyage est en mode "sondage" (dates non fixées)
    /** Photo de couverture (URL vers le store s3) */
    private String coverPhotoUrl;

    // ——— Relations
    /** Élèves inscrits */
    @ManyToMany(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    private List<TripParticipant> participants;

    @ManyToMany(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    private List<Section> sections;

    /** Secteurs (Cycle bac, Post-bac, …) éventuellement vide */
    @ElementCollection(targetClass = Sector.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "trip_sectors", joinColumns = @JoinColumn(name = "trip_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "sector")
    private Set<Sector> sectors = new HashSet<>();

    @ManyToMany(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    private List<User> chaperones; // accompagnateurs

    @ManyToMany(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    private List<DocumentType> mandatoryDocumentTypes; // types de documents obligatoires pour ce voyage

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TripFormality> formalities = new ArrayList<>(); // clonées depuis FormalitePaysTemplate

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TripPreference> preferences = new HashSet<>();

    // Compte “live” des préférences (read-only) calculé par la base
    @Formula("(select count(tp.id) from trip_preferences tp where tp.trip_id = id)")
    private long interestedCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addFormality(TripFormality f) {
        formalities.add(f);
        f.setTrip(this);
    }
}
