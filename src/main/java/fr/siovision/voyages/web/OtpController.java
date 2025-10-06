package fr.siovision.voyages.web;

import fr.siovision.voyages.application.service.OtpService;
import fr.siovision.voyages.infrastructure.dto.ApiMessage;
import fr.siovision.voyages.infrastructure.dto.authentication.OtpIssueRequest;
import fr.siovision.voyages.infrastructure.dto.authentication.OtpVerifyRequest;
import fr.siovision.voyages.infrastructure.dto.authentication.RefreshResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/otp")
@RequiredArgsConstructor
public class OtpController {
    private final OtpService otpService;

    @PostMapping("/resend")
    public ResponseEntity<ApiMessage> resend(@RequestBody OtpIssueRequest req) {
        // Réponse neutre, même si l'e-mail n'existe pas (optionnel)
        try {
            otpService.resend(req.email());
        } catch (IllegalArgumentException ignored) {
            // Ne divulgue pas l’existence de l’e-mail
        }
        return ResponseEntity.ok(new ApiMessage("If the email exists, an OTP has been sent."));
    }

    @PostMapping("/verify")
    RefreshResponse verify(@Valid @RequestBody OtpVerifyRequest req) {
        return otpService.verifyAccountOtp(req.email(), req.otp());
    }
}
