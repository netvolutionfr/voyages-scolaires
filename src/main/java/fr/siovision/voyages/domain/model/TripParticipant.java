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
public class TripParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cust_seq")
    @SequenceGenerator(name = "cust_seq", sequenceName = "cust_seq")
    private Long id;

    private boolean chaperone;

    private LocalDateTime registrationDate;
    private LocalDateTime decisionDate;
    private RegistrationStatus registrationStatus;
    private String decisionMessage;
    private String adminNotes;

    @ManyToOne
    private Trip trip;

    @ManyToOne
    private Participant participant;

    @OneToMany(mappedBy = "tripParticipant", cascade = CascadeType.ALL)
    private List<Document> documents;

}