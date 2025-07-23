package fr.siovision.voyages.infrastructure.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class VoyageDTO {
    private String nom;
    private String description;
    private String destination;
    private LocalDate dateDepart;
    private LocalDate dateRetour;
}
