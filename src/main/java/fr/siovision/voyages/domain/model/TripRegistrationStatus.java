package fr.siovision.voyages.domain.model;

public enum TripRegistrationStatus {
    PENDING, // Inscription en attente de validation
    VALIDATED, // Inscription validée par l'administrateur
    REJECTED, // Inscription refusée par l'administrateur
    ENROLLED, CONFIRMED, CANCELED // Inscription annulée par le participant ou l'administrateur
}
