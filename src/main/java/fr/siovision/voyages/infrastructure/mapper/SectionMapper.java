package fr.siovision.voyages.infrastructure.mapper;

import fr.siovision.voyages.domain.model.Section;
import fr.siovision.voyages.infrastructure.dto.SectionDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SectionMapper {
    SectionDTO toDTO(Section section);
    Section toEntity(SectionDTO sectionDTO);
}
