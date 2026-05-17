package fr.siovision.voyages.infrastructure.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProfileRequest {
    @Pattern(regexp = "^[MFN]$", message = "Gender must be M, F or N")
    private String gender;

    @Pattern(regexp = "^\\+?[0-9 ().\\-]{7,20}$", message = "Invalid phone number")
    private String telephone;

    private LocalDate birthDate;
    private String section;

    private String parent1FirstName;
    private String parent1LastName;
    @Email private String parent1Email;
    @Pattern(regexp = "^\\+?[0-9 ().\\-]{7,20}$", message = "Invalid phone number")
    private String parent1Telephone;

    private String parent2FirstName;
    private String parent2LastName;
    @Email private String parent2Email;
    @Pattern(regexp = "^\\+?[0-9 ().\\-]{7,20}$", message = "Invalid phone number")
    private String parent2Telephone;
}
