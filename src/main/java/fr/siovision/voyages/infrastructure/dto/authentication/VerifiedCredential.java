package fr.siovision.voyages.infrastructure.dto.authentication;

public record VerifiedCredential(byte[] credentialId, byte[] publicKeyCose, long signCount, java.util.UUID aaguid) {}
