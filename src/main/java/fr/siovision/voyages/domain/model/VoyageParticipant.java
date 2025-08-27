package fr.siovision.voyages.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class VoyageParticipant {
    @Id
    @GeneratedValue
    private Long id;

    private boolean accompagnateur;
    private boolean organisateur;

    private LocalDateTime dateInscription;
    private LocalDateTime dateEngagement;
    private StatutInscription statutInscription;
    private String commentaireDecision;
    private String messageMotivation;

    @ManyToOne
    private Voyage voyage;

    @ManyToOne
    private Participant participant;

    @OneToMany(mappedBy = "voyageParticipant", cascade = CascadeType.ALL)
    private List<Document> documents;

}