package fr.siovision.voyages.application.service.impl;

import fr.siovision.voyages.application.service.JwtService;
import fr.siovision.voyages.application.service.MailService;
import fr.siovision.voyages.application.service.RefreshTokenService;
import fr.siovision.voyages.domain.model.OtpToken;
import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.infrastructure.dto.authentication.RefreshResponse;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceImplVerifyTest {

    @Mock OtpTokenRepository otpRepo;
    @Mock UserRepository userRepo;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock RefreshTokenService refreshTokenService;
    @Mock MailService mailService;

    @InjectMocks
    OtpServiceImpl service;

    private User user;
    private OtpToken pendingToken;
    private static final String EMAIL = "user@example.com";
    private static final String OTP_CODE = "123456";
    private static final String FAKE_ACCESS_TOKEN = "access.token.here";
    private static final String FAKE_REFRESH_TOKEN = "raw-refresh-opaque-token";

    @BeforeEach
    void setUp() {
        // Inject @Value fields that Spring normally injects
        ReflectionTestUtils.setField(service, "otpLength", 6);
        ReflectionTestUtils.setField(service, "otpTtlMinutes", 10L);
        ReflectionTestUtils.setField(service, "maxAttempts", 5);
        ReflectionTestUtils.setField(service, "resendCooldownSeconds", 60L);
        ReflectionTestUtils.setField(service, "accessTtlSeconds", 900L);
        ReflectionTestUtils.setField(service, "refreshTtlSeconds", 2592000L);
        ReflectionTestUtils.setField(service, "resendQuotaCount", 5);
        ReflectionTestUtils.setField(service, "resendQuotaWindowMin", 15);

        user = new User();
        user.setEmail(EMAIL);

        pendingToken = OtpToken.builder()
                .user(user)
                .purpose(OtpToken.Purpose.ACCOUNT_VERIFICATION)
                .codeHash("$2a$encoded")
                .expiresAt(Instant.now().plusSeconds(600))
                .status(OtpToken.Status.PENDING)
                .attempts(0)
                .build();
    }

    @Test
    void verifyAccountOtp_onSuccess_callsRefreshTokenServiceIssue() {
        when(userRepo.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(otpRepo.findLatestPendingForUpdate(eq(user), eq(OtpToken.Purpose.ACCOUNT_VERIFICATION),
                eq(OtpToken.Status.PENDING))).thenReturn(Optional.of(pendingToken));
        when(passwordEncoder.matches(OTP_CODE, pendingToken.getCodeHash())).thenReturn(true);
        when(otpRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateAccessToken(eq(user), anyList())).thenReturn(FAKE_ACCESS_TOKEN);
        when(refreshTokenService.issue(user)).thenReturn(FAKE_REFRESH_TOKEN);

        service.verifyAccountOtp(EMAIL, OTP_CODE);

        // Must call refreshTokenService.issue(), not generateOpaqueToken()
        verify(refreshTokenService, times(1)).issue(user);
        verify(refreshTokenService, never()).generateOpaqueToken();
    }

    @Test
    void verifyAccountOtp_onSuccess_returnsNonNullRefreshToken() {
        when(userRepo.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(otpRepo.findLatestPendingForUpdate(eq(user), eq(OtpToken.Purpose.ACCOUNT_VERIFICATION),
                eq(OtpToken.Status.PENDING))).thenReturn(Optional.of(pendingToken));
        when(passwordEncoder.matches(OTP_CODE, pendingToken.getCodeHash())).thenReturn(true);
        when(otpRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateAccessToken(eq(user), anyList())).thenReturn(FAKE_ACCESS_TOKEN);
        when(refreshTokenService.issue(user)).thenReturn(FAKE_REFRESH_TOKEN);

        RefreshResponse response = service.verifyAccountOtp(EMAIL, OTP_CODE);

        assertThat(response).isNotNull();
        assertThat(response.refresh_token()).isNotNull();
        assertThat(response.refresh_token()).isNotBlank();
    }

    @Test
    void verifyAccountOtp_onSuccess_returnedRefreshTokenMatchesIssuedValue() {
        when(userRepo.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(otpRepo.findLatestPendingForUpdate(eq(user), eq(OtpToken.Purpose.ACCOUNT_VERIFICATION),
                eq(OtpToken.Status.PENDING))).thenReturn(Optional.of(pendingToken));
        when(passwordEncoder.matches(OTP_CODE, pendingToken.getCodeHash())).thenReturn(true);
        when(otpRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateAccessToken(eq(user), anyList())).thenReturn(FAKE_ACCESS_TOKEN);
        when(refreshTokenService.issue(user)).thenReturn(FAKE_REFRESH_TOKEN);

        RefreshResponse response = service.verifyAccountOtp(EMAIL, OTP_CODE);

        assertThat(response.refresh_token()).isEqualTo(FAKE_REFRESH_TOKEN);
    }

    @Test
    void verifyAccountOtp_onSuccess_returnsAccessToken() {
        when(userRepo.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(otpRepo.findLatestPendingForUpdate(eq(user), eq(OtpToken.Purpose.ACCOUNT_VERIFICATION),
                eq(OtpToken.Status.PENDING))).thenReturn(Optional.of(pendingToken));
        when(passwordEncoder.matches(OTP_CODE, pendingToken.getCodeHash())).thenReturn(true);
        when(otpRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateAccessToken(eq(user), anyList())).thenReturn(FAKE_ACCESS_TOKEN);
        when(refreshTokenService.issue(user)).thenReturn(FAKE_REFRESH_TOKEN);

        RefreshResponse response = service.verifyAccountOtp(EMAIL, OTP_CODE);

        assertThat(response.access_token()).isEqualTo(FAKE_ACCESS_TOKEN);
        assertThat(response.token_type()).isEqualTo("Bearer");
    }
}
