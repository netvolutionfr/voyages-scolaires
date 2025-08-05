package fr.siovision.voyages.domain.model;

public enum StatutInscription {
    EN_ATTENTE, // Inscription en attente de validation
    VALIDE, // Inscription validée par l'administrateur
    REFUSEE, // Inscription refusée par l'administrateur
    ANNULEE // Inscription annulée par le participant ou l'administrateur
}
