package fr.siovision.voyages.domain.model;

public enum DocumentStatus {
    MISSING, // Missing document
    PENDING, // Pending validation
    REJECTED, // Document rejected by the administrator
    VALIDATED // Document validated and accepted
}
