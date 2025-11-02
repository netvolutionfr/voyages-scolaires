package fr.siovision.voyages.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class TripUser {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cust_seq")
    @SequenceGenerator(name = "cust_seq", sequenceName = "cust_seq")
    private Long id;

    private boolean chaperone;

    private LocalDateTime registrationDate;
    private LocalDateTime decisionDate;
    private TripRegistrationStatus registrationStatus;
    private String decisionMessage;
    private String adminNotes;

    @ManyToOne
    private Trip trip;

    @ManyToOne
    private User user;
}