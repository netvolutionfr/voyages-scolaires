package fr.siovision.voyages.infrastructure.repository;

import fr.siovision.voyages.domain.model.WebAuthnCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebAuthnCredentialRepository extends JpaRepository<WebAuthnCredential, Long> {
    List<WebAuthnCredential> findAllByUserId(Long userId);
}