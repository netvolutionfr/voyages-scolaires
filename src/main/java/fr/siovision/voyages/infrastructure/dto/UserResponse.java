package fr.siovision.voyages.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private UUID publicId;
    private String email;
    private String lastName;
    private String firstName;
    private String fullName;
    private String telephone;
    private String status;
    private String role; // ADMIN, PARTICIPANT, ORGANISATEUR
}
