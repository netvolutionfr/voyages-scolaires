package fr.siovision.voyages.web;

import fr.siovision.voyages.application.service.StudentHealthFormService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me/health-form")
public class StudentHealthFormController {
    @Autowired
    private StudentHealthFormService studentHealthFormService;

    @GetMapping()
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    public ResponseEntity<String> getStudentHealthForm() {
        String form = studentHealthFormService.getFormByStudent();
        return ResponseEntity.ok(form);
    }

    @PostMapping()
    @PreAuthorize("hasAnyRole('STUDENT', 'PARENT')")
    public ResponseEntity<Void> submitStudentHealthForm(@RequestBody String payload) {
        studentHealthFormService.submitForm(payload);
        return ResponseEntity.ok().build();
    }
}
