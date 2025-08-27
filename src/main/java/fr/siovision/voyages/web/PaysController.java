package fr.siovision.voyages.web;

import fr.siovision.voyages.application.service.PaysService;
import fr.siovision.voyages.infrastructure.dto.PaysDTO;
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
@RequestMapping("/api/pays")
public class PaysController {
    @Autowired
    private PaysService paysService;

    @GetMapping()
    public ResponseEntity<Page<PaysDTO>> list(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "nom", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<PaysDTO> pays = paysService.list(null, null);
        return ResponseEntity.ok(pays);
    }
}
