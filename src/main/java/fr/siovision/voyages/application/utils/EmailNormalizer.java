package fr.siovision.voyages.application.utils;

public final class EmailNormalizer {
    public static String norm(String email){
        return email == null ? null : email.trim().toLowerCase();
    }
}