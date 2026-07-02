package fr.siovision.voyages.application.service.impl;

import fr.siovision.voyages.application.service.JwtService;
import fr.siovision.voyages.application.service.MailService;
import fr.siovision.voyages.application.service.RefreshTokenService;
import fr.siovision.voyages.domain.model.OtpToken;
import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.infrastructure.repository.OtpTokenRepository;
import fr.siovision.voyages.infrastructure.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceImplDeletionTest {

    @Mock OtpTokenRepository otpRepo;
    @Mock UserRepository userRepo;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock RefreshTokenService refreshTokenService;
    @Mock MailService mailService;

    @InjectMocks
    OtpServiceImpl service;

    private User user;
    private static final String OTP_CODE = "123456";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "otpLength", 6);
        ReflectionTestUtils.setField(service, "otpTtlMinutes", 10L);
        ReflectionTestUtils.setField(service, "maxAttempts", 5);
        ReflectionTestUtils.setField(service, "resendCooldownSeconds", 60L);

        user = new User();
        user.setEmail("user@example.com");
    }

    @Test
    void issueDeletionOtp_savesTokenWithAccountDeletionPurpose() {
        when(otpRepo.findLatestPendingForUpdate(user, OtpToken.Purpose.ACCOUNT_DELETION, OtpToken.Status.PENDING))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$encoded");

        service.issueDeletionOtp(user);

        verify(otpRepo).save(argThat(token -> token.getPurpose() == OtpToken.Purpose.ACCOUNT_DELETION
                && token.getStatus() == OtpToken.Status.PENDING
                && token.getUser() == user));
        verify(mailService).sendOtpEmail(eq(user), anyString(), eq(10L));
    }

    @Test
    void issueDeletionOtp_respectsCooldown() {
        OtpToken recent = OtpToken.builder()
                .user(user).purpose(OtpToken.Purpose.ACCOUNT_DELETION)
                .codeHash("x").status(OtpToken.Status.PENDING)
                .createdAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600))
                .attempts(0).build();
        when(otpRepo.findLatestPendingForUpdate(user, OtpToken.Purpose.ACCOUNT_DELETION, OtpToken.Status.PENDING))
                .thenReturn(Optional.of(recent));

        assertThatThrownBy(() -> service.issueDeletionOtp(user))
                .isInstanceOf(OtpServiceImpl.TooManyRequestsException.class);

        verify(mailService, never()).sendOtpEmail(any(), any(), anyLong());
    }

    @Test
    void verifyDeletionOtp_onValidCode_marksTokenUsedAndDoesNotIssueSession() {
        OtpToken pending = OtpToken.builder()
                .user(user).purpose(OtpToken.Purpose.ACCOUNT_DELETION)
                .codeHash("$2a$encoded").status(OtpToken.Status.PENDING)
                .expiresAt(Instant.now().plusSeconds(600)).attempts(0).build();
        when(otpRepo.findLatestPendingForUpdate(user, OtpToken.Purpose.ACCOUNT_DELETION, OtpToken.Status.PENDING))
                .thenReturn(Optional.of(pending));
        when(passwordEncoder.matches(OTP_CODE, pending.getCodeHash())).thenReturn(true);

        service.verifyDeletionOtp(user, OTP_CODE);

        assertThat(pending.getStatus()).isEqualTo(OtpToken.Status.USED);
        assertThat(pending.getConsumedAt()).isNotNull();
        verifyNoInteractions(jwtService, refreshTokenService);
    }

    @Test
    void verifyDeletionOtp_onInvalidCode_throwsAndDoesNotConsumeToken() {
        OtpToken pending = OtpToken.builder()
                .user(user).purpose(OtpToken.Purpose.ACCOUNT_DELETION)
                .codeHash("$2a$encoded").status(OtpToken.Status.PENDING)
                .expiresAt(Instant.now().plusSeconds(600)).attempts(0).build();
        when(otpRepo.findLatestPendingForUpdate(user, OtpToken.Purpose.ACCOUNT_DELETION, OtpToken.Status.PENDING))
                .thenReturn(Optional.of(pending));
        when(passwordEncoder.matches(OTP_CODE, pending.getCodeHash())).thenReturn(false);

        assertThatThrownBy(() -> service.verifyDeletionOtp(user, OTP_CODE))
                .isInstanceOf(OtpServiceImpl.InvalidOtpException.class);

        assertThat(pending.getStatus()).isEqualTo(OtpToken.Status.PENDING);
    }

    @Test
    void verifyDeletionOtp_onExpiredToken_throws() {
        OtpToken expired = OtpToken.builder()
                .user(user).purpose(OtpToken.Purpose.ACCOUNT_DELETION)
                .codeHash("$2a$encoded").status(OtpToken.Status.PENDING)
                .expiresAt(Instant.now().minusSeconds(1)).attempts(0).build();
        when(otpRepo.findLatestPendingForUpdate(user, OtpToken.Purpose.ACCOUNT_DELETION, OtpToken.Status.PENDING))
                .thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.verifyDeletionOtp(user, OTP_CODE))
                .isInstanceOf(OtpServiceImpl.InvalidOtpException.class);

        assertThat(expired.getStatus()).isEqualTo(OtpToken.Status.EXPIRED);
    }

    @Test
    void verifyDeletionOtp_withoutPendingToken_throws() {
        when(otpRepo.findLatestPendingForUpdate(user, OtpToken.Purpose.ACCOUNT_DELETION, OtpToken.Status.PENDING))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyDeletionOtp(user, OTP_CODE))
                .isInstanceOf(OtpServiceImpl.InvalidOtpException.class);
    }
}
