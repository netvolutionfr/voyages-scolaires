package fr.siovision.voyages.web;

import fr.siovision.voyages.domain.exception.ProfilNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class RestExceptionHandler {
    @ExceptionHandler(ProfilNotFoundException.class)
    public ResponseEntity<?> handleProductNotFoundException(ProfilNotFoundException e) {
        return ResponseEntity
                .status(404)
                .body(Map.of("error", e.getMessage()));
    }
}