package fr.siovision.voyages.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.siovision.voyages.domain.model.StudentHealthForm;
import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.infrastructure.repository.StudentHealthFormRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentHealthFormService {
    private final StudentHealthFormRepository studentHealthFormRepository;
    private final CurrentUserService currentUserService;
    private final HealthFormPayloadRenderer healthFormPayloadRenderer;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public String getFormByStudent() {
        User user = currentUserService.getCurrentUser();
        StudentHealthForm form = studentHealthFormRepository.findByStudentId(user.getId());
        return healthFormPayloadRenderer.render(form);
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
        form.setSignedAt(Instant.now());
        // extract validUntil from payload if present (format "validUntil": "2024-12-31")
        try {
            JsonNode rootNode = objectMapper.readTree(jsonPayload);
            if (rootNode.has("validUntil")) {
                String validUntilStr = rootNode.get("validUntil").asText();
                Instant validUntil = Instant.parse(validUntilStr + "T00:00:00Z");
                form.setValidUntil(validUntil);
            }
        } catch (Exception e) {
            log.warn("Failed to parse validUntil from health form payload", e);
        }
        studentHealthFormRepository.save(form);
    }

    /** Nettoyage périodique des formulaires expirés */
    /* utilise le champ validUntil pour déterminer l’expiration */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public int purgeExpired() {
        int deletedCount = studentHealthFormRepository.deleteByValidUntilBefore(Instant.now());
        log.info("Purged {} expired student health forms", deletedCount);
        return deletedCount;
    }
}
