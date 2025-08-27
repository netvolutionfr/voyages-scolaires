package fr.siovision.voyages.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;



@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class Voyage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    private String nom;
    private String description;
    private String destination;
    @ManyToOne(fetch = FetchType.LAZY)
    private Pays pays;
    private LocalDate dateDepart;
    private LocalDate dateRetour;
    private Integer nombreMinParticipants;
    private Integer nombreMaxParticipants;
    private LocalDate dateDebutInscription;
    private LocalDate dateFinInscription;

    @ManyToMany(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    private List<VoyageParticipant> participants;

    @ManyToMany(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    private List<Section> sections;

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

    public void removeFormalite(FormaliteVoyage f) {
        formalites.remove(f);
        f.setVoyage(null);
    }
}
