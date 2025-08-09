package fr.siovision.voyages.web;

import fr.siovision.voyages.application.service.UserService;
import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.infrastructure.dto.ParticipantProfileResponse;
import fr.siovision.voyages.infrastructure.dto.UserResponse;
import fr.siovision.voyages.infrastructure.dto.UserTelephoneRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class UserController {
    @Autowired
    private UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        UserResponse user = userService.getOrCreateUserFromToken(jwt);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/me")
    public ResponseEntity<UserResponse> updateTelephone(@AuthenticationPrincipal Jwt jwt, @RequestBody UserTelephoneRequest request) {
        UserResponse user = userService.updateUserTelephone(jwt, request);
        return ResponseEntity.ok(user);
    }
}
