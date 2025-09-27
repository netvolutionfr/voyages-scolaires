package fr.siovision.voyages.application.service.impl;

import fr.siovision.voyages.application.service.ChallengeService;
import fr.siovision.voyages.config.AppProps;
import fr.siovision.voyages.domain.model.RegistrationAttempt;
import fr.siovision.voyages.infrastructure.dto.authentication.ChallengeResponse;
import fr.siovision.voyages.infrastructure.repository.RegistrationAttemptRepository;
import fr.siovision.voyages.application.utils.Base64Url;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

@Service
public class ChallengeServiceImpl implements ChallengeService {
    private final RegistrationAttemptRepository attempts;
    private final AppProps props;
    private final SecureRandom rng = new SecureRandom();

    public ChallengeServiceImpl(RegistrationAttemptRepository attempts, AppProps props) {
        this.attempts = attempts; this.props = props;
    }

    @Override @Transactional
    public ChallengeResponse issue() {
        UUID uuid = UUID.randomUUID();
        byte[] chal = new byte[32]; rng.nextBytes(chal);
        var att = new RegistrationAttempt();
        att.setUuid(uuid);
        att.setChallenge(chal);
        att.setRpId(props.rp().id());
        att.setOrigin(props.rp().origin());
        att.setExpiresAt(Instant.now().plusSeconds(props.challenge().ttlSeconds()));
        att.setUsed(false);
        attempts.save(att);

        return new ChallengeResponse(
                uuid.toString(),
                Base64Url.encode(chal),
                props.rp().id(),
                props.rp().name(),
                props.challenge().timeoutMs()
        );
    }}
