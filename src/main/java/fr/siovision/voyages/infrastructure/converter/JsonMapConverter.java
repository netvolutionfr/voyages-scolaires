package fr.siovision.voyages.infrastructure.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.LinkedHashMap;
import java.util.Map;

@Converter
public class JsonMapConverter implements AttributeConverter<Map<String, Object>, String> {
    private static final tools.jackson.databind.ObjectMapper MAPPER = tools.jackson.databind.json.JsonMapper.builder().build();
    @Override public String convertToDatabaseColumn(Map<String, Object> attribute) {
        try {
            if (attribute == null || attribute.isEmpty()) return null;
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) { throw new IllegalArgumentException("JSON serialize error", e); }
    }
    @Override public Map<String, Object> convertToEntityAttribute(String dbData) {
        try {
            if (dbData == null || dbData.isBlank()) return new LinkedHashMap<>();
            return MAPPER.readValue(dbData, new tools.jackson.core.type.TypeReference<Map<String,Object>>(){});
        } catch (Exception e) { throw new IllegalArgumentException("JSON parse error", e); }
    }
}