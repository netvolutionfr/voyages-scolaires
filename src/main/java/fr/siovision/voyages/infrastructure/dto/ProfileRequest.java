package fr.siovision.voyages.infrastructure.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ProfileRequest {
    private String gender; // M, F, N
    private String telephone;
    private LocalDate birthDate;
    private String section;

    private String parent1FirstName;
    private String parent1LastName;
    private String parent1Email;
    private String parent1Telephone;

    private String parent2FirstName;
    private String parent2LastName;
    private String parent2Email;
    private String parent2Telephone;
}
