package fr.siovision.voyages.web;

import fr.siovision.voyages.application.security.TripSecurity;
import fr.siovision.voyages.application.service.TripRegistrationAdminService;
import fr.siovision.voyages.application.service.TripRegistrationService;
import fr.siovision.voyages.infrastructure.dto.RegistrationAdminUpdateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for TripRegistrationController.update (PATCH /{tripUserId}).
 *
 * These tests verify two things from the security fix:
 * 1) The PATCH handler takes no tripId @RequestParam — a request without ?tripId=... must NOT return 400.
 * 2) The @PreAuthorize expression uses canViewRegistration, not canViewTrip.
 *
 * Method security (@PreAuthorize) is evaluated on the Spring bean proxy.
 * For routing and parameter tests we use standaloneSetup (no Spring context).
 * For the @PreAuthorize annotation test we use reflection to read the annotation value directly.
 */
@ExtendWith(MockitoExtension.class)
class TripRegistrationControllerTest {

    @Mock
    TripRegistrationAdminService adminService;

    @Mock
    TripRegistrationService registrationService;

    @Mock
    TripSecurity tripSecurity;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TripRegistrationController controller =
                new TripRegistrationController(registrationService, adminService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    // --- Routing: PATCH without ?tripId param must NOT return 400 ---

    @Test
    void patch_withoutTripIdQueryParam_isRoutedSuccessfully() throws Exception {
        when(adminService.updateStatus(anyLong(), any())).thenReturn(
                new RegistrationAdminUpdateResponse(1L, 10L, 5L, "VALIDATED"));

        // Before the fix, the handler required @RequestParam Long tripId → 400 if absent.
        // After the fix, no such param → should reach the handler and return 200.
        mockMvc.perform(patch("/api/trips/registrations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"VALIDATED\",\"adminMessage\":\"OK\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void patch_withSuperfluousTripIdQueryParam_isStillRouted() throws Exception {
        when(adminService.updateStatus(anyLong(), any())).thenReturn(
                new RegistrationAdminUpdateResponse(1L, 10L, 5L, "VALIDATED"));

        // A stray ?tripId=... should not cause a 400 either (it's simply ignored)
        mockMvc.perform(patch("/api/trips/registrations/1?tripId=10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"VALIDATED\",\"adminMessage\":\"OK\"}"))
                .andExpect(status().isOk());
    }

    // --- @PreAuthorize annotation uses canViewRegistration, not canViewTrip ---

    @Test
    void patchHandler_preAuthorizeAnnotation_usesCanViewRegistrationNotCanViewTrip()
            throws NoSuchMethodException {
        Method updateMethod = TripRegistrationController.class.getMethod(
                "update", Long.class,
                fr.siovision.voyages.infrastructure.dto.RegistrationAdminUpdateRequest.class);

        org.springframework.security.access.prepost.PreAuthorize preAuthorize =
                updateMethod.getAnnotation(
                        org.springframework.security.access.prepost.PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        String expression = preAuthorize.value();

        assertThat(expression).contains("canViewRegistration");
        assertThat(expression).doesNotContain("canViewTrip");
    }

    @Test
    void patchHandler_preAuthorizeAnnotation_requiresAdminOrTeacherRole()
            throws NoSuchMethodException {
        Method updateMethod = TripRegistrationController.class.getMethod(
                "update", Long.class,
                fr.siovision.voyages.infrastructure.dto.RegistrationAdminUpdateRequest.class);

        org.springframework.security.access.prepost.PreAuthorize preAuthorize =
                updateMethod.getAnnotation(
                        org.springframework.security.access.prepost.PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        String expression = preAuthorize.value();

        assertThat(expression).containsAnyOf("ADMIN", "TEACHER");
    }
}
