package fr.siovision.voyages.infrastructure.repository;

import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.domain.model.WebAuthnCredential;
import jakarta.annotation.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WebAuthnCredentialRepository extends JpaRepository<WebAuthnCredential, Long> {
    List<WebAuthnCredential> findAllByUserId(Long userId);

    Object findByUser(User user);

    Optional<WebAuthnCredential> findByCredentialId(@Nullable byte[] credentialId);
}