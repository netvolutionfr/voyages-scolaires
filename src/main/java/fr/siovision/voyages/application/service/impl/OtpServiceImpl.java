package fr.siovision.voyages.application.service.impl;

import fr.siovision.voyages.application.service.OtpService;
import fr.siovision.voyages.infrastructure.dto.authentication.VerifyOtpRequest;
import fr.siovision.voyages.infrastructure.dto.authentication.VerifyOtpResponse;
import org.springframework.stereotype.Service;

@Service
public class OtpServiceImpl implements OtpService {
    @Override
    public void issueAndSend(String email) {
        // TODO Auto-generated method stub

    }

    @Override
    public VerifyOtpResponse verify(VerifyOtpRequest req) {
        // TODO Auto-generated method stub
        return null;
    }
}
