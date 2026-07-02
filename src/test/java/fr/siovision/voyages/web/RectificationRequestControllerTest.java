package fr.siovision.voyages.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.siovision.voyages.application.service.RectificationRequestService;
import fr.siovision.voyages.domain.model.RectificationStatus;
import fr.siovision.voyages.infrastructure.dto.rectification.RectificationRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RectificationRequestControllerTest {

    @Mock
    RectificationRequestService rectificationRequestService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RectificationRequestController controller = new RectificationRequestController(rectificationRequestService);
        // Spring Boot enregistre automatiquement JavaTimeModule et le module Jackson de
        // Spring Data pour Page<T> ; hors contexte Spring (standalone), on les charge ici.
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .registerModule(new org.springframework.data.web.config.SpringDataJacksonConfiguration.PageModule(null));
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void submit_returnsCreatedWithBody() throws Exception {
        RectificationRequestDTO dto = new RectificationRequestDTO(
                UUID.randomUUID(), "LAST_NAME", "Martin", "Mariage", "PENDING",
                Instant.now(), null, null);
        when(rectificationRequestService.submit(any())).thenReturn(dto);

        mockMvc.perform(post("/api/me/rectification-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"field\":\"LAST_NAME\",\"requestedValue\":\"Martin\",\"reason\":\"Mariage\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void list_returnsPageFromService() throws Exception {
        when(rectificationRequestService.listByStatus(eq(RectificationStatus.PENDING), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/users/rectification-requests"))
                .andExpect(status().isOk());
    }

    @Test
    void resolve_returnsOk() throws Exception {
        RectificationRequestDTO dto = new RectificationRequestDTO(
                UUID.randomUUID(), "LAST_NAME", "Martin", "Mariage", "APPLIED",
                Instant.now(), Instant.now(), "Fait");
        when(rectificationRequestService.resolve(any(), any())).thenReturn(dto);

        mockMvc.perform(patch("/api/users/rectification-requests/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPLIED\",\"adminComment\":\"Fait\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void submitHandler_requiresAuthentication() throws NoSuchMethodException {
        Method handler = RectificationRequestController.class.getMethod(
                "submit", fr.siovision.voyages.infrastructure.dto.rectification.RectificationRequestCreateRequest.class);
        PreAuthorize preAuthorize = handler.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("isAuthenticated()");
    }

    @Test
    void listAndResolveHandlers_requireAdminRole() throws NoSuchMethodException {
        Method listHandler = RectificationRequestController.class.getMethod(
                "list", RectificationStatus.class, org.springframework.data.domain.Pageable.class);
        Method resolveHandler = RectificationRequestController.class.getMethod(
                "resolve", UUID.class,
                fr.siovision.voyages.infrastructure.dto.rectification.RectificationRequestResolveRequest.class);

        assertThat(listHandler.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('ADMIN')");
        assertThat(resolveHandler.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('ADMIN')");
    }
}
