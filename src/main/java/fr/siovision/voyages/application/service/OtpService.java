package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.infrastructure.dto.authentication.RefreshResponse;

public interface OtpService {
    void issueAndSend(User user);
    RefreshResponse verifyAccountOtp(String email, String otpCode);
    void resend(String email);
}