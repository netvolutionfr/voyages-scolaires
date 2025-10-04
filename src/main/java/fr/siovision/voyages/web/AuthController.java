package fr.siovision.voyages.web;

import com.webauthn4j.data.AuthenticationRequest;
import com.webauthn4j.data.PublicKeyCredentialCreationOptions;
import com.webauthn4j.data.PublicKeyCredentialRequestOptions;
import fr.siovision.voyages.infrastructure.dto.authentication.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import fr.siovision.voyages.application.service.AuthenticationService;
import fr.siovision.voyages.application.service.ChallengeService;
import fr.siovision.voyages.application.service.OtpService;
import fr.siovision.voyages.application.service.RegistrationFlowService;

@Validated
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {
    private final ChallengeService challengeService;
    private final RegistrationFlowService registrationFlowService;
    private final OtpService otpService;
    private final AuthenticationService authenticationService;

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
    JwtResponse finishAuthn(@Valid @RequestBody AuthenticationRequest AuthenticationRequest, HttpServletRequest httpRequest) {
        String appOrigin = httpRequest.getHeader("Origin");
        return authenticationService.finish(AuthenticationRequest , appOrigin);
    }}
