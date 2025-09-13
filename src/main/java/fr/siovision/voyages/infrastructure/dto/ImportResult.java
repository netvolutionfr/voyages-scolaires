package fr.siovision.voyages.infrastructure.dto;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ImportResult {

    // getters
    private int imported = 0;
    private int skipped = 0;
    private List<LineError> errors = new ArrayList<>();

    public void incImported() { imported++; }
    public void incSkipped() { skipped++; }

    public void addError(int line, String message) {
        errors.add(new LineError(line, message));
    }

    public record LineError(int line, String error) {}

}
