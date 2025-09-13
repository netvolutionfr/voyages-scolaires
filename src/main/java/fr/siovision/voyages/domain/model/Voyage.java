package fr.siovision.voyages.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;


@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class Voyage {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cust_seq")
    @SequenceGenerator(name = "cust_seq", sequenceName = "cust_seq")
    private Long id;

    @EqualsAndHashCode.Include
    @Column(nullable = false, updatable = false, unique = true)
    private UUID publicId = UUID.randomUUID();

    private String nom;
    private String description;
    private String destination;

    private Integer prixTotal; // en centimes d'euros
    private Integer participationDesFamilles; // en centimes d'euros

    @ManyToOne(fetch = FetchType.LAZY)
    private Pays pays;

    private LocalDate dateDepart;
    private LocalDate dateRetour;
    private Integer nombreMinParticipants;
    private Integer nombreMaxParticipants;
    private LocalDate dateDebutInscription;
    private LocalDate dateFinInscription;

    /** Photo de couverture (URL vers le store s3) */
    private String coverPhotoUrl;

    // ——— Relations
    /** Élèves inscrits */
    @ManyToMany(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    private List<VoyageParticipant> participants;

    @ManyToMany(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    private List<Section> sections;

    /** Secteurs (Cycle bac, Post-bac, …) éventuellement vide */
    @ElementCollection(targetClass = Secteur.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "voyage_secteurs", joinColumns = @JoinColumn(name = "voyage_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "secteur")
    private Set<Secteur> secteurs = new HashSet<>();

    @ManyToMany(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    private List<User> organisateurs;

    @ManyToMany(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    private List<TypeDocument> documentsObligatoires;

    @OneToMany(mappedBy = "voyage", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<FormaliteVoyage> formalites = new ArrayList<>(); // clonées depuis FormalitePaysTemplate

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

    public void addFormalite(FormaliteVoyage f) {
        formalites.add(f);
        f.setVoyage(this);
    }
}
