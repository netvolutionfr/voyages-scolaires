package fr.siovision.voyages.infrastructure.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

@Converter
public class StringSetCsvConverter implements AttributeConverter<Set<String>, String> {
    @Override public String convertToDatabaseColumn(Set<String> attribute) {
        if (attribute == null || attribute.isEmpty()) return null;
        return String.join(",", attribute);
    }
    @Override public Set<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return new HashSet<>();
        String[] parts = dbData.split(",");
        Set<String> set = new LinkedHashSet<>();
        for (String p : parts) { set.add(p.trim()); }
        return set;
    }
}