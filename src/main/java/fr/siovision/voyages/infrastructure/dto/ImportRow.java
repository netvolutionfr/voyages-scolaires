package fr.siovision.voyages.infrastructure.dto;

public record ImportRow(
        String role,
        String nom,
        String prenom,
        String email,
        String telephone,
        String parent1Nom,
        String parent1Prenom,
        String parent1Email,
        String parent1Tel,
        String parent2Nom,
        String parent2Prenom,
        String parent2Email,
        String parent2Tel
) {}