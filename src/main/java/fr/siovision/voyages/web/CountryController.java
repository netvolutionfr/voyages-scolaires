package fr.siovision.voyages.web;

import fr.siovision.voyages.application.service.CountryService;
import fr.siovision.voyages.infrastructure.dto.CountryDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/country")
public class CountryController {
    @Autowired
    private CountryService paysService;

    @GetMapping()
    public ResponseEntity<Page<CountryDTO>> list(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<CountryDTO> countries = paysService.list(q, pageable);
        return ResponseEntity.ok(countries);
    }
}
