package fr.siovision.voyages.web;

import fr.siovision.voyages.application.service.CookieFactory;
import fr.siovision.voyages.application.service.CurrentUserService;
import fr.siovision.voyages.application.service.GdprExportService;
import fr.siovision.voyages.application.service.OtpService;
import fr.siovision.voyages.application.service.UserErasureService;
import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.infrastructure.dto.ApiMessage;
import fr.siovision.voyages.infrastructure.dto.gdpr.AccountDeletionConfirmRequest;
import fr.siovision.voyages.infrastructure.dto.gdpr.GdprExportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class GdprController {

    private final GdprExportService gdprExportService;
    private final CurrentUserService currentUserService;
    private final OtpService otpService;
    private final UserErasureService userErasureService;
    private final CookieFactory cookieFactory;

    @GetMapping("/data-export")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GdprExportResponse> exportMyData() {
        GdprExportResponse export = gdprExportService.export();
        String filename = "voyages-export-" + export.profile().publicId() + ".json";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(export);
    }

    /** ADR-0004 : première étape de l'effacement — envoie un OTP de confirmation par email. */
    @PostMapping("/delete-request")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiMessage> requestAccountDeletion() {
        User user = currentUserService.getCurrentUser();
        otpService.issueDeletionOtp(user);
        return ResponseEntity.ok(new ApiMessage("A confirmation code has been sent to your email address."));
    }

    /** Effacement définitif (Art. 17 RGPD), confirmé par le code OTP émis via /delete-request. */
    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteMyAccount(@RequestBody AccountDeletionConfirmRequest request) {
        User user = currentUserService.getCurrentUser();
        otpService.verifyDeletionOtp(user, request.otp());
        userErasureService.eraseSelf(user);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.clearRefreshCookie().toString())
                .build();
    }
}
