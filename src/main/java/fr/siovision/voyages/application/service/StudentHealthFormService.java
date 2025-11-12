package fr.siovision.voyages.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

    @Transactional
    public String getFormByStudent() {
        User user = currentUserService.getCurrentUser();
        StudentHealthForm form = studentHealthFormRepository.findByStudentId(user.getId());

        // update payload to add updatedAt field if form exists
        if (form != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode rootNode = objectMapper.readTree(form.getPayload());
                ((ObjectNode) rootNode).put("updatedAt", Instant.now().toString());
                return objectMapper.writeValueAsString(rootNode);
            } catch (Exception e) {
                log.warn("Failed to add updatedAt to health form payload", e);
            }
            return form.getPayload();
        }
        return null;
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
            ObjectMapper objectMapper = new ObjectMapper();
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
