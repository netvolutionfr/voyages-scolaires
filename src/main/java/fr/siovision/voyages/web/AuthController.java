package fr.siovision.voyages.web;


import fr.siovision.voyages.application.security.AuthUtils;
import fr.siovision.voyages.application.service.ActivationTokenService;
import fr.siovision.voyages.application.service.MailService;
import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.infrastructure.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final ActivationTokenService tokenService;
    private final MailService mailService;

    public record RegisterRequest(@NotBlank @Email String email) {}
    public record ActivateRequest(@NotBlank String token) {}
    public record OkResponse(boolean ok) {}

    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken token) {
        return token;
    }

    @PostMapping("/register-request")
    public ResponseEntity<OkResponse> registerRequest(@RequestBody RegisterRequest req) {
        // Recherche silencieuse (pour ne pas divulguer qui existe)
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(req.email());
        if (userOpt.isPresent()) {
            var user = userOpt.get();
            var token = tokenService.generateForUser(user.getPublicId().toString(), user.getEmail());
            mailService.sendActivationLink(user.getEmail(), token);
        }
        // Toujours 200
        return ResponseEntity.ok(new OkResponse(true));
    }

    @PostMapping("/activate")
    public ResponseEntity<OkResponse> activate(@RequestBody ActivateRequest req, HttpServletRequest request) {
        var tok = tokenService.verify(req.token()); // throws on invalid/expired
        var user = userRepository.findByPublicId(UUID.fromString(tok.publicId()))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Construire une Authentication sans mot de passe
        var auth = new AbstractAuthenticationToken(AuthUtils.authoritiesFor(user)) {
            @Override public Object getCredentials() { return ""; }
            @Override public Object getPrincipal() { return user; }
        };
        auth.setAuthenticated(true);
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(auth);
        request.getSession(true); // assure un JSESSIONID et lie le SecurityContext à la session

        return ResponseEntity.ok(new OkResponse(true));
    }
}
