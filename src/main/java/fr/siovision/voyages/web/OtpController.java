package fr.siovision.voyages.web;

import fr.siovision.voyages.application.service.CookieFactory;
import fr.siovision.voyages.application.service.OtpService;
import fr.siovision.voyages.config.AppProps;
import fr.siovision.voyages.infrastructure.dto.ApiMessage;
import fr.siovision.voyages.infrastructure.dto.authentication.OtpIssueRequest;
import fr.siovision.voyages.infrastructure.dto.authentication.OtpVerifyRequest;
import fr.siovision.voyages.infrastructure.dto.authentication.RefreshCookieResponse;
import fr.siovision.voyages.infrastructure.dto.authentication.RefreshResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
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
    private final CookieFactory cookieFactory;
    private final AppProps appProps;

    @Value("${app.jwt.refresh-ttl-seconds}")
    long refreshTtlSec;

    @PostMapping("/resend")
    public ResponseEntity<ApiMessage> resend(@Valid @RequestBody OtpIssueRequest req) {
        try {
            otpService.resend(req.email());
        } catch (IllegalArgumentException ignored) {
            // Ne divulgue pas l'existence de l'e-mail
        }
        return ResponseEntity.ok(new ApiMessage("If the email exists, an OTP has been sent."));
    }

    @PostMapping("/verify")
    public ResponseEntity<RefreshCookieResponse> verify(@Valid @RequestBody OtpVerifyRequest req) {
        RefreshResponse result = otpService.verifyAccountOtp(req.email(), req.otp());
        var refreshCookie = cookieFactory.refreshCookie(result.refresh_token(), refreshTtlSec);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(new RefreshCookieResponse("Bearer", result.access_token(), appProps.jwt().accessTtlSeconds()));
    }
}
