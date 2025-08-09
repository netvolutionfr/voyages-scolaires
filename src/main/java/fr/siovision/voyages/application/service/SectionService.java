package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.model.Section;
import fr.siovision.voyages.infrastructure.dto.SectionDTO;
import fr.siovision.voyages.infrastructure.repository.SectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class SectionService {
    @Autowired
    private SectionRepository sectionRepository;

    public SectionDTO getSectionById(Long id) {
        return sectionRepository.findById(id)
                .map(section -> new SectionDTO(section.getId(), section.getLibelle(), section.getDescription()))
                .orElse(null);
    }

    public SectionDTO createSection(SectionDTO sectionDTO) {
        // Convert DTO to entity and save
        System.out.println(sectionDTO);
        Section section = new Section();
        section.setLibelle(sectionDTO.getLibelle());
        section.setDescription(sectionDTO.getDescription());
        Section savedSection = sectionRepository.save(section);

        // Convert back to DTO
        return new SectionDTO(savedSection.getId(), savedSection.getLibelle(), savedSection.getDescription());
    }

    public SectionDTO updateSection(Long id, SectionDTO sectionDTO) {
        // Find the existing section
        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Section not found with id: " + id));

        // Update fields
        section.setLibelle(sectionDTO.getLibelle());
        section.setDescription(sectionDTO.getDescription());

        // Save and return updated DTO
        Section updatedSection = sectionRepository.save(section);
        return new SectionDTO(updatedSection.getId(), updatedSection.getLibelle(), updatedSection.getDescription());
    }

    public Page<SectionDTO> list(String q, Pageable pageable) {
        Page<Section> sections = sectionRepository.search(q, pageable);
        return sections.map(section -> new SectionDTO(section.getId(), section.getLibelle(), section.getDescription()));
    }

    public void deleteSection(Long id) {
        // Check if the section exists
        if (!sectionRepository.existsById(id)) {
            throw new IllegalArgumentException("Section not found with id: " + id);
        }

        // Delete the section
        sectionRepository.deleteById(id);
    }
}
