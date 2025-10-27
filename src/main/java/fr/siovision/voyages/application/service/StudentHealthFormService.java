package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.model.StudentHealthForm;
import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.infrastructure.repository.StudentHealthFormRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentHealthFormService {
    private final StudentHealthFormRepository studentHealthFormRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public String getFormByStudent() {
        User user = currentUserService.getCurrentUser();
        StudentHealthForm form = studentHealthFormRepository.findByStudentId(user.getId());
        return form != null ? form.getPayload() : "{}";
    }

    @Transactional
    public void submitForm(String jsonPayload) {
        User user = currentUserService.getCurrentUser();
        StudentHealthForm form = studentHealthFormRepository.findByStudentId(user.getId());
        if (form == null) {
            form = new StudentHealthForm();
            form.setStudent(user);
        }
        form.setPayload(jsonPayload);
        studentHealthFormRepository.save(form);
    }
}
