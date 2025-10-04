package fr.siovision.voyages.web;

import com.webauthn4j.data.AuthenticationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import com.webauthn4j.data.PublicKeyCredentialCreationOptions;
import com.webauthn4j.data.PublicKeyCredentialRequestOptions;
import fr.siovision.voyages.application.service.*;
import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.infrastructure.dto.authentication.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@Validated
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {
    private final ChallengeService challengeService;
    private final RegistrationFlowService registrationFlowService;
    private final OtpService otpService;
    private final AuthenticationService authenticationService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;

    @Value("${app.jwt.access-ttl-seconds}")
    long accessTtlSec;

    @Value("${app.jwt.refresh-ttl-seconds}")
    long refreshTtlSec;

    // Pour iOS 26+, requête de création de passkey sans email
    @GetMapping("/webauthn/register/options")
    PublicKeyCredentialCreationOptions challengeIOS(HttpServletRequest httpRequest) {
        String origin = httpRequest.getHeader("Origin");
        return challengeService.issueIOS(origin);
    }

    // Pour les autres plateformes, requête de création de passkey avec email
    @PostMapping("/webauthn/register/options")
    PublicKeyCredentialCreationOptions challenge(@Valid @RequestBody EmailHint req, HttpServletRequest httpRequest) {
        String origin = httpRequest.getHeader("Origin");
        return challengeService.issue(req.email(), origin);
    }

    @PostMapping("/webauthn/register/finish-onestep")
    RegisterFinishResponse finishRegisterOneStep(@Valid @RequestBody RegisterFinishRequest registerFinishRequest, HttpServletRequest httpRequest) {
        String origin = httpRequest.getHeader("Origin");
        return registrationFlowService.finishRegistrationOneStep(registerFinishRequest, origin);
    }

    @PostMapping("/webauthn/register/finish")
    RegisterFinishResponse finishRegister(@Valid @RequestBody String registrationRequest, HttpServletRequest httpRequest) {
        String origin = httpRequest.getHeader("Origin");
        return registrationFlowService.finishRegistration(registrationRequest, origin);
    }

    @PostMapping("/auth/verify-otp")
    VerifyOtpResponse verify(@Valid @RequestBody VerifyOtpRequest req) {
        return otpService.verify(req);
    }

    @GetMapping("/webauthn/authenticate/options")
    PublicKeyCredentialRequestOptions authnOptionsIOS(HttpServletRequest httpRequest) {
        String origin = httpRequest.getHeader("Origin");
        return challengeService.issueAuthIOS(origin);
    }

    @PostMapping("/webauthn/authenticate/finish")
    AuthResponse finishAuthn(@Valid @RequestBody AuthenticationRequest AuthenticationRequest, HttpServletRequest httpRequest) {
        String appOrigin = httpRequest.getHeader("Origin");
        return authenticationService.finish(AuthenticationRequest , appOrigin);
    }

    @PostMapping("/api/auth/refresh")
    public ResponseEntity<RefreshResponse> refresh(@RequestBody RefreshRequest req, @AuthenticationPrincipal Jwt jwt) {
        // 1) Rotation sécurisée du refresh
        String newRefresh = refreshTokenService.rotate(req.refresh_token());

        // 2) Générer un nouvel access
        User currentUser = userService.getUserByJwt(jwt);
        String newAccess = jwtService.generateAccessToken(currentUser);

        // 3) Réponse
        return ResponseEntity.ok(new RefreshResponse(
                "Bearer", newAccess, accessTtlSec, newRefresh, refreshTtlSec
        ));
    }
}
