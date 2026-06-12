package fr.siovision.voyages.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.siovision.voyages.domain.model.StudentHealthForm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class HealthFormPayloadRenderer {

    private final ObjectMapper objectMapper;

    public String render(StudentHealthForm form) {
        if (form == null) {
            return null;
        }

        String payload = form.getPayload();
        if (payload == null || payload.isBlank() || form.getUpdatedAt() == null) {
            return payload;
        }

        try {
            JsonNode rootNode = objectMapper.readTree(payload);
            if (rootNode instanceof ObjectNode objectNode) {
                Instant updatedAt = form.getUpdatedAt();
                objectNode.put("updatedAt", updatedAt.toString());
                return objectMapper.writeValueAsString(objectNode);
            }
        } catch (Exception e) {
            log.warn("Failed to render health form payload with persisted updatedAt", e);
        }

        return payload;
    }
}
