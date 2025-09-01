package fr.siovision.voyages.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TypeDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cust_seq")
    @SequenceGenerator(name = "cust_seq", sequenceName = "cust_seq", allocationSize = 50)
    private Long id;

    private String abr; // Abréviation du type de document (ex: "cni", "passeport", "visa")
    private String nom; // Nom du document (ex: "Autorisation parentale", "Certificat médical")
    private String description; // Description du document (optionnel)
}
