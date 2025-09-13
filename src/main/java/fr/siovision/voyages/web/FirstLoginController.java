package fr.siovision.voyages.web;

import fr.siovision.voyages.application.service.FirstLoginService;
import fr.siovision.voyages.infrastructure.dto.FirstLoginRequest;
import fr.siovision.voyages.infrastructure.dto.FirstLoginResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public")
public class FirstLoginController {
    @Autowired
    private final FirstLoginService firstLoginService;

    public FirstLoginController(FirstLoginService firstLoginService) {
        this.firstLoginService = firstLoginService;
    }

    @PostMapping("/first-login")
    public ResponseEntity<FirstLoginResponse> firstLogin(@Valid @RequestBody FirstLoginRequest req,
                                                         @RequestHeader(value = "X-Request-Id", required = false) String reqId) {
        firstLoginService.handleFirstLogin(req.email().trim(), reqId);
        // Toujours réponse générique
        return ResponseEntity.ok(FirstLoginResponse.generic());
    }
}
