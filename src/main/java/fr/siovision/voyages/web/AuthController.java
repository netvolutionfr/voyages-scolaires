package fr.siovision.voyages.web;

import fr.siovision.voyages.infrastructure.dto.authentication.*;
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

    @GetMapping("/auth/challenge")
    ChallengeResponse challenge() {
        return challengeService.issue();
    }

    @PostMapping("/webauthn/register/finish")
    RegisterFinishResponse finishRegister(@Valid @RequestBody RegisterFinishRequest req) {
        return registrationFlowService.finishRegistration(req);
    }

    @PostMapping("/auth/verify-otp")
    VerifyOtpResponse verify(@Valid @RequestBody VerifyOtpRequest req) {
        return otpService.verify(req);
    }

    @PostMapping("/webauthn/authenticate/options")
    AuthnOptionsResponse authnOptions(@RequestBody(required=false) EmailHint req) {
        return authenticationService.options(req);
    }

    @PostMapping("/webauthn/authenticate/finish")
    JwtResponse finishAuthn(@Valid @RequestBody AuthnFinishRequest req) {
        return authenticationService.finish(req);
    }}
