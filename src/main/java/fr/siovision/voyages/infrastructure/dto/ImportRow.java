package fr.siovision.voyages.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImportRow {
    private String role;
    private String lastName;
    private String firstName;
    private String email;
    private String telephone;
    private String gender;
    private String section;
    private String birthDate;
    private String parent1LastName;
    private String parent1FirstName;
    private String parent1Email;
    private String parent1Tel;
    private String parent2LastName;
    private String parent2FirstName;
    private String parent2Email;
    private String parent2Tel;
}