package fr.siovision.voyages.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "trip_preferences",
        uniqueConstraints = @UniqueConstraint(columnNames = {"trip_id", "user_id"}))
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class TripPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String interest; // "YES" or "NO"
}
