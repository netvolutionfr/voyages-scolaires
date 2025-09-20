package fr.siovision.voyages.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "voyage_preferences",
        uniqueConstraints = @UniqueConstraint(columnNames = {"voyage_id", "user_id"}))
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class VoyagePreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "voyage_id")
    private Voyage voyage;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String interest; // "YES" ou "NO"

    // getters/setters
}
