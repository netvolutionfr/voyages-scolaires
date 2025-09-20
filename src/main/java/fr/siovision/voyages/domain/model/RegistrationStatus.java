package fr.siovision.voyages.domain.model;

public enum RegistrationStatus {
    PENDING, // Inscription en attente de validation
    VALIDATED, // Inscription validée par l'administrateur
    REJECTED, // Inscription refusée par l'administrateur
    CANCELED // Inscription annulée par le participant ou l'administrateur
}
