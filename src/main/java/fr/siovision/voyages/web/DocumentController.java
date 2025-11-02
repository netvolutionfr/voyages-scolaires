package fr.siovision.voyages.web;

import fr.siovision.voyages.application.service.DocumentsService;
import fr.siovision.voyages.infrastructure.dto.DocumentsDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/me/documents")
public class DocumentController {
    @Autowired
    private DocumentsService documentsService;

    @GetMapping()
    public ResponseEntity<DocumentsDTO> getDocuments() {
        DocumentsDTO documents = documentsService.list();
        return ResponseEntity.ok(documents);
    }
}
