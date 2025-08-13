package fr.siovision.voyages.web;

import fr.siovision.voyages.application.service.UserService;
import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.infrastructure.dto.ParticipantProfileResponse;
import fr.siovision.voyages.infrastructure.dto.UserResponse;
import fr.siovision.voyages.infrastructure.dto.UserTelephoneRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<Page<UserResponse>> getUsers(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "nom", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<UserResponse> users = userService.getAllUsers(jwt, q, pageable);
        return ResponseEntity.ok(users);
    }
}
