package fr.siovision.voyages.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChaperoneDTO {
    private UUID publicId;
    private String lastName;
    private String firstName;
    private String email;
    private String telephone;
}
