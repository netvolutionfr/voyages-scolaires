package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.infrastructure.dto.ProfileUpdateRequest;
import fr.siovision.voyages.infrastructure.mapper.UserMapper;
import fr.siovision.voyages.infrastructure.repository.SectionRepository;
import fr.siovision.voyages.infrastructure.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class UserServiceUpdateOwnProfileTest {

    @Mock UserRepository userRepository;
    @Mock UserMapper userMapper;
    @Mock SectionRepository sectionRepository;
    @Mock UserErasureService userErasureService;

    private UserService userService;
    private User user;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, userMapper, sectionRepository, userErasureService);

        UUID publicId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        user = new User();
        user.setPublicId(publicId);
        user.setTelephone("0600000000");
        user.setDisplayName("Old Name");
        user.setGender("M");

        jwt = Jwt.withTokenValue("token")
                .header("alg", "ES256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(900))
                .claims(c -> c.putAll(Map.of("sub", publicId.toString())))
                .build();

        lenient().when(userRepository.findByPublicId(publicId)).thenReturn(Optional.of(user));
    }

    @Test
    void updateOwnProfile_updatesOnlyProvidedFields() {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setTelephone("0601020304");
        // displayName et gender laissés à null : ne doivent pas être modifiés

        userService.updateOwnProfile(jwt, request);

        assertThat(user.getTelephone()).isEqualTo("0601020304");
        assertThat(user.getDisplayName()).isEqualTo("Old Name");
        assertThat(user.getGender()).isEqualTo("M");
    }

    @Test
    void updateOwnProfile_rejectsInvalidGender() {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setGender("X");

        assertThatThrownBy(() -> userService.updateOwnProfile(jwt, request))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(user.getGender()).isEqualTo("M");
    }

    @Test
    void updateOwnProfile_cannotChangeIdentityFields() {
        // ProfileUpdateRequest n'expose structurellement ni firstName, ni lastName,
        // ni birthDate, ni email : le self-service ne peut pas les modifier.
        var componentNames = java.util.Arrays.stream(ProfileUpdateRequest.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName)
                .toList();

        assertThat(componentNames).doesNotContain("firstName", "lastName", "birthDate", "email");
    }
}
