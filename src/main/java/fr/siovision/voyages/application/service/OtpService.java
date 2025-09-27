package fr.siovision.voyages.application.service;

import fr.siovision.voyages.infrastructure.dto.authentication.VerifyOtpRequest;
import fr.siovision.voyages.infrastructure.dto.authentication.VerifyOtpResponse;

public interface OtpService {
    void issueAndSend(String email);
    VerifyOtpResponse verify(VerifyOtpRequest req);
}