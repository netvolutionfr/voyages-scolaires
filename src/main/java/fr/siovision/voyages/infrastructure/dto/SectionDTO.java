package fr.siovision.voyages.infrastructure.dto;

import fr.siovision.voyages.domain.model.Cycle;
import fr.siovision.voyages.domain.model.YearTag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SectionDTO {
    private Long id;
    private UUID publicId;
    private String label;
    private String description;
    private Cycle cycle;     // COLLEGE, LYCEE_GENERAL, ...
    private YearTag year;    // _3e, _2nde, _1ere, Tle, BTS1, ...
    private boolean isActive;
}
