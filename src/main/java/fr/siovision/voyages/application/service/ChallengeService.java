package fr.siovision.voyages.application.service;

import fr.siovision.voyages.infrastructure.dto.authentication.ChallengeResponse;

public interface ChallengeService {
    ChallengeResponse issue();
}