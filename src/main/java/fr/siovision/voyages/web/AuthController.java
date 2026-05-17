package fr.siovision.voyages.web;

import com.webauthn4j.data.PublicKeyCredentialCreationOptions;
import com.webauthn4j.data.PublicKeyCredentialRequestOptions;
import fr.siovision.voyages.application.service.*;
import fr.siovision.voyages.config.AppProps;
import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.infrastructure.dto.authentication.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Optional;

@Validated
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {
    private final ChallengeService challengeService;
    private final RegistrationFlowService registrationFlowService;
    private final AuthenticationService authenticationService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final CookieFactory cookieFactory;
    private final AppProps appProps;

    @Value("${app.jwt.refresh-ttl-seconds}")
    long refreshTtlSec;

    @GetMapping("/webauthn/register/options")
    PublicKeyCredentialCreationOptions challengeIOS(HttpServletRequest httpRequest) {
        return challengeService.issueIOS(httpRequest.getHeader("Origin"));
    }

    @PostMapping("/webauthn/register/options")
    PublicKeyCredentialCreationOptions challenge(@Valid @RequestBody EmailHint req, HttpServletRequest httpRequest) {
        return challengeService.issue(req.email(), httpRequest.getHeader("Origin"));
    }

    // PENDING token — retourné en body, pas de cookie refresh (pas encore de compte actif)
    @PostMapping("/webauthn/register/finish-onestep")
    RegisterFinishResponse finishRegisterOneStep(@Valid @RequestBody RegisterFinishRequest req, HttpServletRequest httpRequest) {
        return registrationFlowService.finishRegistrationOneStep(req, httpRequest.getHeader("Origin"));
    }

    @PostMapping("/webauthn/register/finish")
    RegisterFinishResponse finishRegister(@Valid @RequestBody String registrationRequest, HttpServletRequest httpRequest) {
        return registrationFlowService.finishRegistration(registrationRequest, httpRequest.getHeader("Origin"));
    }

    @GetMapping("/webauthn/authenticate/options")
    PublicKeyCredentialRequestOptions authnOptionsIOS(HttpServletRequest httpRequest) {
        return challengeService.issueAuthIOS(httpRequest.getHeader("Origin"));
    }

    @PostMapping("/webauthn/authenticate/finish")
    public ResponseEntity<AuthCookieResponse> finishAuthn(
            @Valid @RequestBody AuthnFinishRequest req,
            HttpServletRequest httpRequest) {
        AuthResponse result = authenticationService.finish(req, httpRequest.getHeader("Origin"));
        var refreshCookie = cookieFactory.refreshCookie(result.refreshToken(), refreshTtlSec);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(new AuthCookieResponse(
                        "Bearer",
                        result.accessToken(),
                        appProps.jwt().accessTtlSeconds(),
                        result.user()));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<RefreshCookieResponse> refresh(HttpServletRequest request) {
        String presented = Arrays.stream(Optional.ofNullable(request.getCookies()).orElse(new Cookie[0]))
                .filter(c -> "refresh_token".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Missing refresh_token cookie"));

        RotateResult rotateResult = refreshTokenService.rotate(presented);
        User currentUser = rotateResult.user();
        String newAccess = jwtService.generateAccessToken(currentUser);

        var refreshCookie = cookieFactory.refreshCookie(rotateResult.newRefreshToken(), refreshTtlSec);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(new RefreshCookieResponse("Bearer", newAccess, appProps.jwt().accessTtlSeconds()));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        Arrays.stream(Optional.ofNullable(request.getCookies()).orElse(new Cookie[0]))
                .filter(c -> "refresh_token".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .ifPresent(refreshTokenService::revoke);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.clearRefreshCookie().toString())
                .build();
    }
}
