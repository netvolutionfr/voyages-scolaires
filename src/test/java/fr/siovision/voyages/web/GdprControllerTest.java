package fr.siovision.voyages.web;

import fr.siovision.voyages.application.service.CookieFactory;
import fr.siovision.voyages.application.service.CurrentUserService;
import fr.siovision.voyages.application.service.GdprExportService;
import fr.siovision.voyages.application.service.OtpService;
import fr.siovision.voyages.application.service.UserErasureService;
import fr.siovision.voyages.domain.exception.ErasureBlockedException;
import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.domain.model.UserRole;
import fr.siovision.voyages.infrastructure.dto.gdpr.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GdprControllerTest {

    @Mock GdprExportService gdprExportService;
    @Mock CurrentUserService currentUserService;
    @Mock OtpService otpService;
    @Mock UserErasureService userErasureService;
    @Mock CookieFactory cookieFactory;

    private MockMvc mockMvc;
    private User user;

    @BeforeEach
    void setUp() {
        GdprController controller = new GdprController(
                gdprExportService, currentUserService, otpService, userErasureService, cookieFactory);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        user = new User();
        user.setPublicId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        user.setRole(UserRole.PARENT);

        lenient().when(currentUserService.getCurrentUser()).thenReturn(user);
        lenient().when(cookieFactory.clearRefreshCookie())
                .thenReturn(ResponseCookie.from("refresh_token", "").path("/api/auth").maxAge(0).build());
    }

    @Test
    void dataExport_returnsAttachmentWithUserPublicIdInFilename() throws Exception {
        UUID publicId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        GdprProfileDTO profile = new GdprProfileDTO(
                publicId, "eleve@example.com", "Alice", "Dupont", "Alice Dupont",
                "F", "2010-05-01", "0601020304", "STUDENT", "ACTIVE", "6eA",
                null, null
        );
        GdprExportResponse response = new GdprExportResponse(
                Instant.parse("2026-07-02T10:00:00Z"),
                "voyages-gdpr-export/v1",
                profile,
                new GdprConsentDTO(null, null),
                null,
                List.of(),
                new GdprTripsDTO(List.of(), List.of()),
                List.of(),
                null,
                new GdprSecurityDTO(List.of(), null)
        );

        when(gdprExportService.export()).thenReturn(response);

        mockMvc.perform(get("/api/me/data-export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"voyages-export-" + publicId + ".json\""))
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(content().json("{\"format\":\"voyages-gdpr-export/v1\"}"));
    }

    @Test
    void dataExportHandler_requiresAuthentication() throws NoSuchMethodException {
        Method handler = GdprController.class.getMethod("exportMyData");

        org.springframework.security.access.prepost.PreAuthorize preAuthorize =
                handler.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("isAuthenticated()");
    }

    @Test
    void requestAccountDeletion_issuesOtpForCurrentUser() throws Exception {
        mockMvc.perform(post("/api/me/delete-request"))
                .andExpect(status().isOk());

        verify(otpService).issueDeletionOtp(user);
    }

    @Test
    void deleteMyAccount_verifiesOtpThenErasesAndClearsCookie() throws Exception {
        mockMvc.perform(delete("/api/me")
                        .contentType("application/json")
                        .content("{\"otp\":\"123456\"}"))
                .andExpect(status().isNoContent())
                .andExpect(header().exists("Set-Cookie"));

        verify(otpService).verifyDeletionOtp(user, "123456");
        verify(userErasureService).eraseSelf(user);
    }

    @Test
    void deleteMyAccount_doesNotEraseWhenOtpInvalid() throws Exception {
        doThrow(new fr.siovision.voyages.application.service.impl.OtpServiceImpl.InvalidOtpException("Invalid code."))
                .when(otpService).verifyDeletionOtp(any(), any());

        mockMvc.perform(delete("/api/me")
                        .contentType("application/json")
                        .content("{\"otp\":\"000000\"}"))
                .andExpect(status().isBadRequest());

        verify(userErasureService, never()).eraseSelf(any());
    }

    @Test
    void deleteMyAccount_propagatesErasureGuardRailAs409() throws Exception {
        doThrow(new ErasureBlockedException("guardian_of_active_student"))
                .when(userErasureService).eraseSelf(any());

        mockMvc.perform(delete("/api/me")
                        .contentType("application/json")
                        .content("{\"otp\":\"123456\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().json("{\"error\":\"guardian_of_active_student\"}"));
    }

    @Test
    void deleteRequestAndDeleteHandlers_requireAuthentication() throws NoSuchMethodException {
        Method deleteRequestHandler = GdprController.class.getMethod("requestAccountDeletion");
        Method deleteHandler = GdprController.class.getMethod(
                "deleteMyAccount", fr.siovision.voyages.infrastructure.dto.gdpr.AccountDeletionConfirmRequest.class);

        assertThat(deleteRequestHandler.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class).value())
                .isEqualTo("isAuthenticated()");
        assertThat(deleteHandler.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class).value())
                .isEqualTo("isAuthenticated()");
    }
}
