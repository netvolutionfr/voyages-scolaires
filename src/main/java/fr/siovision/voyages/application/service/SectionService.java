package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.model.Cycle;
import fr.siovision.voyages.domain.model.Section;
import fr.siovision.voyages.domain.model.YearTag;
import fr.siovision.voyages.infrastructure.dto.SectionDTO;
import fr.siovision.voyages.infrastructure.mapper.SectionMapper;
import fr.siovision.voyages.infrastructure.repository.SectionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SectionService {
    private final SectionRepository sectionRepository;
    private final SectionMapper sectionMapper;

    @Transactional(readOnly = true)
    public SectionDTO getSectionById(Long id) {
        Objects.requireNonNull(id, "id ne peut pas être null");
        return sectionRepository.findById(id)
                .map(sectionMapper::toDTO)
                .orElseThrow(() -> new EntityNotFoundException("Section non trouvée id=" + id));
    }

    public SectionDTO createSection(SectionDTO sectionDTO) {
        Objects.requireNonNull(sectionDTO, "sectionDTO ne peut pas être null");

        sectionDTO.setId(null);

        Section section = sectionMapper.toEntity(sectionDTO);
        Section saved = sectionRepository.save(section);

        return sectionMapper.toDTO(saved);
    }

    @Transactional
    public SectionDTO updateSection(Long id, SectionDTO sectionDTO) {
        Objects.requireNonNull(id, "id ne peut pas être null");
        Objects.requireNonNull(sectionDTO, "sectionDTO ne peut pas être null");

        Section updated = sectionRepository.findById(id)
                .map(existing -> {
                    existing.setPublicId(sectionDTO.getPublicId());
                    existing.setLabel(sectionDTO.getLabel());
                    existing.setDescription(sectionDTO.getDescription());
                    existing.setCycle(sectionDTO.getCycle());
                    existing.setYear(sectionDTO.getYear());
                    existing.setActive(sectionDTO.isActive());
                    return sectionRepository.save(existing);
                })
                .orElseThrow(() -> new EntityNotFoundException("Section non trouvée id=" + id));

        log.info("Section mise à jour id={}", updated.getId());
        return sectionMapper.toDTO(updated);
    }

    @Transactional(readOnly = true)
    public Page<SectionDTO> list(
            String q,
            Cycle cycle,
            YearTag year,
            boolean activeOnly,
            String sortKey,
            Pageable pageable
    ) {
        final String qLower     = (q == null || q.isBlank()) ? null : q.trim().toLowerCase(Locale.ROOT);
        final String qLike  = (qLower == null) ? null : "%" + qLower + "%";
        final String cycleParam = (cycle == null) ? null : cycle.name();
        final String yearParam  = (year  == null) ? null : year.name();

        Pageable p = "yearLabel".equalsIgnoreCase(sortKey)
                ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()) // on neutralise le sort externe
                : (pageable.getSort().isUnsorted()
                ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("label").ascending())
                : pageable);

        boolean useYearLabel = "yearLabel".equalsIgnoreCase(sortKey);

        return sectionRepository.searchSections(qLike, cycleParam, yearParam, activeOnly, useYearLabel, p)
                .map(sectionMapper::toDTO);
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
}
