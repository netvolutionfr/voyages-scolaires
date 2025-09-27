package fr.siovision.voyages.infrastructure.repository;

import fr.siovision.voyages.domain.model.WebAuthnCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WebAuthnCredentialRepository extends JpaRepository<WebAuthnCredential, Long> {
    Optional<WebAuthnCredential> findByCredentialId(String credentialIdB64u);
    List<WebAuthnCredential> findAllByUserId(Long userId);
}