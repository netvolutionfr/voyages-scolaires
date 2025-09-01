package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.model.Section;
import fr.siovision.voyages.infrastructure.dto.SectionDTO;
import fr.siovision.voyages.infrastructure.repository.SectionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class SectionService {
    private final SectionRepository sectionRepository;

    @Transactional(readOnly = true)
    public SectionDTO getSectionById(Long id) {
        Objects.requireNonNull(id, "id ne peut pas être null");
        return sectionRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Section non trouvée id=" + id));
    }

    public SectionDTO createSection(SectionDTO sectionDTO) {
        Objects.requireNonNull(sectionDTO, "sectionDTO ne peut pas être null");
        Section section = toEntity(sectionDTO);
        Section saved = sectionRepository.save(section);
        log.info("Section créée id={}", saved.getId());
        return toDto(saved);
    }

    @Transactional
    public SectionDTO updateSection(Long id, SectionDTO sectionDTO) {
        Objects.requireNonNull(id, "id ne peut pas être null");
        Objects.requireNonNull(sectionDTO, "sectionDTO ne peut pas être null");

        Section updated = sectionRepository.findById(id)
                .map(existing -> {
                    existing.setLibelle(sectionDTO.getLibelle());
                    existing.setDescription(sectionDTO.getDescription());
                    return sectionRepository.save(existing);
                })
                .orElseThrow(() -> new EntityNotFoundException("Section non trouvée id=" + id));

        log.info("Section mise à jour id={}", updated.getId());
        return toDto(updated);
    }

    @Transactional(readOnly = true)
    public Page<SectionDTO> list(String q, Pageable pageable) {
        String query = q == null ? "" : q.trim();
        Page<Section> sections = sectionRepository.search(query, pageable);
        return sections.map(this::toDto);
    }

    @Transactional
    public void deleteSection(Long id) {
        Objects.requireNonNull(id, "id ne peut pas être null");
        if (!sectionRepository.existsById(id)) {
            throw new EntityNotFoundException("Section non trouvée id=" + id);
        }
        sectionRepository.deleteById(id);
        log.info("Section supprimée id={}", id);
    }

    private SectionDTO toDto(Section section) {
        return new SectionDTO(section.getId(), section.getLibelle(), section.getDescription());
    }

    private Section toEntity(SectionDTO dto) {
        Section section = new Section();
        section.setLibelle(dto.getLibelle());
        section.setDescription(dto.getDescription());
        return section;
    }
}
