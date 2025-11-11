package fr.siovision.voyages.web;

import fr.siovision.voyages.application.service.UserService;
import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.domain.model.UserRole;
import fr.siovision.voyages.infrastructure.dto.UserCreateRequest;
import fr.siovision.voyages.infrastructure.dto.UserResponse;
import fr.siovision.voyages.infrastructure.dto.UserTelephoneRequest;
import fr.siovision.voyages.infrastructure.dto.UserUpdateRequest;
import fr.siovision.voyages.infrastructure.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserController {
    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        User user = userService.getUserByJwt(jwt);
        UserResponse userResponse = userMapper.toDTO(user);
        return ResponseEntity.ok(userResponse);
    }

    @PostMapping("/me")
    public ResponseEntity<UserResponse> updateTelephone(@AuthenticationPrincipal Jwt jwt, @RequestBody UserTelephoneRequest request) {
        UserResponse user = userService.updateUserTelephone(jwt, request);
        return ResponseEntity.ok(user);
    }

    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @GetMapping("/users")
    public ResponseEntity<Page<UserResponse>> getUsers(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(value = "role.in", required = false) List<UserRole> roles,
            @PageableDefault(size = 20, sort = "lastName", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<UserResponse> users = userService.getAllUsers(jwt, roles, pageable);
        return ResponseEntity.ok(users);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userPublicId}")
    public ResponseEntity<UserResponse> getUserByPublicId(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("userPublicId") String userPublicId
    ) {
        User user = userService.getUserByPublicId(jwt, userPublicId);
        UserResponse userResponse = userMapper.toDTO(user);
        if (userResponse == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(userResponse);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/users/{userPublicId}")
    public ResponseEntity<String> updateUserByPublicId(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("userPublicId") String userPublicId,
            @RequestBody UserUpdateRequest request
    ) {
        UserResponse updatedUser = userService.updateUser(jwt, userPublicId, request);
        return ResponseEntity.status(HttpStatus.OK).body(updatedUser.getPublicId().toString());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users")
    public ResponseEntity<String> createUser(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UserCreateRequest request
    ) {
        User createdUser = userService.createUser(jwt, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser.getPublicId().toString());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/users/{userPublicId}")
    public ResponseEntity<Void> deleteUserByPublicId(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("userPublicId") String userPublicId
    ) {
        userService.deleteUser(jwt, userPublicId);
        return ResponseEntity.noContent().build();
    }
}
