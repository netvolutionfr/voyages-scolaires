package fr.siovision.voyages.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TypeDocumentDTO {
    private Long id;

    private String abr; // Abréviation du type de document (ex: "cni", "passeport", "visa")
    private String nom; // Nom du document (ex: "Autorisation parentale", "Certificat médical")
    private String description; // Description du document (optionnel)
}
