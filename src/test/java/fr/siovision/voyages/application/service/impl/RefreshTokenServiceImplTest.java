package fr.siovision.voyages.application.service.impl;

import fr.siovision.voyages.domain.exception.UnauthorizedException;
import fr.siovision.voyages.domain.model.RefreshToken;
import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.infrastructure.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    RefreshTokenRepository repo;

    @InjectMocks
    RefreshTokenServiceImpl service;

    private User user;
    private String rawToken;
    private byte[] hash;
    private UUID familyId;

    @BeforeEach
    void setUp() {
        user = new User();
        rawToken = service.generateOpaqueToken();
        hash = service.sha256(rawToken);
        familyId = UUID.randomUUID();
    }

    private RefreshToken buildToken(String status, Instant expiresAt) {
        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setTokenHash(hash);
        rt.setStatus(status);
        rt.setExpiresAt(expiresAt);
        rt.setFamilyId(familyId);
        return rt;
    }

    // --- ROTATED token → UnauthorizedException + family revoked ---

    @Test
    void rotate_withRotatedToken_throwsUnauthorizedException() {
        RefreshToken rotated = buildToken("ROTATED", Instant.now().plusSeconds(3600));
        when(repo.findByHashForUpdate(any())).thenReturn(Optional.of(rotated));

        assertThatExceptionOfType(UnauthorizedException.class)
                .isThrownBy(() -> service.rotate(rawToken));
    }

    @Test
    void rotate_withRotatedToken_revokesEntireFamily() {
        RefreshToken rotated = buildToken("ROTATED", Instant.now().plusSeconds(3600));
        when(repo.findByHashForUpdate(any())).thenReturn(Optional.of(rotated));

        assertThatExceptionOfType(UnauthorizedException.class)
                .isThrownBy(() -> service.rotate(rawToken));

        verify(repo).revokeFamily(familyId);
    }

    @Test
    void rotate_withRotatedToken_exceptionMessageDoesNotLeakSpecificReason() {
        RefreshToken rotated = buildToken("ROTATED", Instant.now().plusSeconds(3600));
        when(repo.findByHashForUpdate(any())).thenReturn(Optional.of(rotated));

        Throwable thrown = catchThrowable(() -> service.rotate(rawToken));

        assertThat(thrown).isInstanceOf(UnauthorizedException.class);
        String msg = thrown.getMessage() == null ? "" : thrown.getMessage().toLowerCase();
        // The exception message must not contain diagnostic reasons that would leak
        // token state details to the client. "invalid_token" is the only acceptable value.
        assertThat(msg).doesNotContain("reused", "revoked", "expired");
        assertThat(msg).doesNotContain("family");
    }

    // --- REVOKED token → UnauthorizedException + family revoked ---

    @Test
    void rotate_withRevokedToken_throwsUnauthorizedException() {
        RefreshToken revoked = buildToken("REVOKED", Instant.now().plusSeconds(3600));
        when(repo.findByHashForUpdate(any())).thenReturn(Optional.of(revoked));

        assertThatExceptionOfType(UnauthorizedException.class)
                .isThrownBy(() -> service.rotate(rawToken));
    }

    @Test
    void rotate_withRevokedToken_revokesEntireFamily() {
        RefreshToken revoked = buildToken("REVOKED", Instant.now().plusSeconds(3600));
        when(repo.findByHashForUpdate(any())).thenReturn(Optional.of(revoked));

        assertThatExceptionOfType(UnauthorizedException.class)
                .isThrownBy(() -> service.rotate(rawToken));

        verify(repo).revokeFamily(familyId);
    }

    @Test
    void rotate_withRevokedToken_exceptionMessageDoesNotLeakSpecificReason() {
        RefreshToken revoked = buildToken("REVOKED", Instant.now().plusSeconds(3600));
        when(repo.findByHashForUpdate(any())).thenReturn(Optional.of(revoked));

        Throwable thrown = catchThrowable(() -> service.rotate(rawToken));

        assertThat(thrown).isInstanceOf(UnauthorizedException.class);
        String msg = thrown.getMessage() == null ? "" : thrown.getMessage().toLowerCase();
        // Must not expose token state details; generic code only
        assertThat(msg).doesNotContain("reused", "revoked", "expired");
        assertThat(msg).doesNotContain("family");
    }

    // --- Expired ACTIVE token → UnauthorizedException ---

    @Test
    void rotate_withExpiredToken_throwsUnauthorizedException() {
        RefreshToken expired = buildToken("ACTIVE", Instant.now().minusSeconds(1));
        when(repo.findByHashForUpdate(any())).thenReturn(Optional.of(expired));

        assertThatExceptionOfType(UnauthorizedException.class)
                .isThrownBy(() -> service.rotate(rawToken));
    }

    @Test
    void rotate_withExpiredToken_exceptionMessageDoesNotLeakSpecificReason() {
        RefreshToken expired = buildToken("ACTIVE", Instant.now().minusSeconds(1));
        when(repo.findByHashForUpdate(any())).thenReturn(Optional.of(expired));

        Throwable thrown = catchThrowable(() -> service.rotate(rawToken));

        assertThat(thrown).isInstanceOf(UnauthorizedException.class);
        String msg = thrown.getMessage() == null ? "" : thrown.getMessage().toLowerCase();
        // Must not expose diagnostic token state; only generic code is acceptable
        assertThat(msg).doesNotContain("reused", "revoked", "expired");
        assertThat(msg).doesNotContain("family");
    }

    @Test
    void rotate_withExpiredToken_doesNotRevokeFamilyForNormalExpiry() {
        RefreshToken expired = buildToken("ACTIVE", Instant.now().minusSeconds(1));
        when(repo.findByHashForUpdate(any())).thenReturn(Optional.of(expired));

        assertThatExceptionOfType(UnauthorizedException.class)
                .isThrownBy(() -> service.rotate(rawToken));

        // A simple expiry does NOT revoke the whole family — only reuse does
        verify(repo, never()).revokeFamily(any());
    }

    // --- Unknown token → UnauthorizedException ---

    @Test
    void rotate_withUnknownToken_throwsUnauthorizedException() {
        when(repo.findByHashForUpdate(any())).thenReturn(Optional.empty());

        assertThatExceptionOfType(UnauthorizedException.class)
                .isThrownBy(() -> service.rotate(rawToken));
    }

    // --- Happy path smoke test: valid ACTIVE token succeeds ---

    @Test
    void rotate_withValidActiveToken_returnsNewToken() {
        RefreshToken active = buildToken("ACTIVE", Instant.now().plusSeconds(3600));
        when(repo.findByHashForUpdate(any())).thenReturn(Optional.of(active));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.rotate(rawToken);

        assertThat(result).isNotNull();
        assertThat(result.newRefreshToken()).isNotBlank();
        assertThat(result.user()).isSameAs(user);
    }
}
