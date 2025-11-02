package fr.siovision.voyages.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DocumentType {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cust_seq")
    @SequenceGenerator(name = "cust_seq", sequenceName = "cust_seq")
    private Long id;

    private String abr; // Abréviation du type de document (ex: "cni", "passeport", "visa")
    private String label; // Nom du document (ex: "Autorisation parentale", "Certificat médical")
    private String description; // Description du document (optionnel)

    @Column(nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private DocumentScope scope = DocumentScope.TRIP;
}
