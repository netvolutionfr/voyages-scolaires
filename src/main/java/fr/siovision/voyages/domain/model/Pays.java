package fr.siovision.voyages.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class Pays {
    @Id
    @GeneratedValue
    private Long id;
    private String nom;

    @OneToMany(mappedBy = "pays",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<FormalitePaysTemplate> formalitesPays = new ArrayList<>();
}
