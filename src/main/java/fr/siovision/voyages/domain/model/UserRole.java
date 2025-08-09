package fr.siovision.voyages.domain.model;

public enum UserRole {
    ADMIN, // Administrateur du système
    PARENT, // Parent ou tuteur légal
    STUDENT, // Élève ou étudiant
    TEACHER, // Enseignant ou professeur
    UNKNOWN // Rôle inconnu ou non spécifié
}
