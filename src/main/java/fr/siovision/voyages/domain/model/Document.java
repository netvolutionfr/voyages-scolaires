package fr.siovision.voyages.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cust_seq")
    @SequenceGenerator(name = "cust_seq", sequenceName = "cust_seq", allocationSize = 50)
    private Long id;

    private String fichierNom;
    private String fichierType;
    private Long fichierTaille;
    private String fichierUrl;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private TypeDocument typeDocument;

    @Enumerated(EnumType.STRING)
    private EtatDocument etatDocument;

    private String numero;
    private LocalDate dateEmission;
    private LocalDate dateExpiration;

    @ManyToOne
    @JoinColumn(nullable = false)
    private VoyageParticipant voyageParticipant;

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
}
