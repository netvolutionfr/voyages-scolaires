package fr.siovision.voyages.web;

import fr.siovision.voyages.application.service.SectionService;
import fr.siovision.voyages.domain.model.Cycle;
import fr.siovision.voyages.domain.model.YearTag;
import fr.siovision.voyages.infrastructure.dto.SectionDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/sections")
public class SectionController {
    @Autowired
    private SectionService sectionService;

    @GetMapping()
    public ResponseEntity<Page<SectionDTO>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Cycle cycle,
            @RequestParam(required = false) YearTag year,
            @RequestParam(defaultValue = "true") boolean activeOnly,
            // sortKey: "yearLabel" (défaut) ou "label" ou "description" etc.
            @RequestParam(defaultValue = "yearLabel") String sortKey,
            @PageableDefault(size = 50) Pageable pageable
    ) {
        Page<SectionDTO> sections = sectionService.list(q, cycle, year, activeOnly, sortKey, pageable);
        return ResponseEntity.ok(sections);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SectionDTO> getSectionById(@PathVariable Long id) {
        SectionDTO section = sectionService.getSectionById(id);
        return section != null ? ResponseEntity.ok(section) : ResponseEntity.notFound().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<Void> createSection(@RequestBody SectionDTO sectionDTO) {
        SectionDTO created = sectionService.createSection(sectionDTO);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.getId()).toUri()).build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<SectionDTO> updateSection(@PathVariable Long id, @RequestBody SectionDTO sectionDTO) {
        return ResponseEntity.ok(sectionService.updateSection(id, sectionDTO));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSection(@PathVariable Long id) {
        sectionService.deleteSection(id);
        return ResponseEntity.noContent().build();
    }
}
