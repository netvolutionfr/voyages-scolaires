package fr.siovision.voyages.web;

import fr.siovision.voyages.application.service.GdprExportService;
import fr.siovision.voyages.infrastructure.dto.gdpr.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GdprControllerTest {

    @Mock
    GdprExportService gdprExportService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        GdprController controller = new GdprController(gdprExportService);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
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
}
