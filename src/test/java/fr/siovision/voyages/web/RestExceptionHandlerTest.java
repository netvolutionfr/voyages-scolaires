package fr.siovision.voyages.web;

import fr.siovision.voyages.domain.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for RestExceptionHandler using standalone MockMvc setup.
 * This avoids Spring Boot context loading entirely and wires only the handler
 * we care about — suitable since RestExceptionHandler has no Spring-injected deps.
 */
class RestExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ThrowingController())
                .setControllerAdvice(new RestExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    // ---- Fake controller to trigger specific exceptions ----

    @RestController
    @RequestMapping("/test-exceptions")
    static class ThrowingController {

        @GetMapping("/unauthorized")
        public String throwUnauthorized() {
            throw new UnauthorizedException();
        }

        @GetMapping("/illegal-argument")
        public String throwIllegalArgument() {
            throw new IllegalArgumentException("internal detail that must not leak");
        }

        @GetMapping("/runtime")
        public String throwRuntime() {
            throw new RuntimeException("internal server detail");
        }

        @PostMapping("/validation")
        public String handleValidation(@Valid @RequestBody ValidatedBody body) {
            return "ok";
        }
    }

    record ValidatedBody(@NotBlank String name) {}

    // ---- Tests ----

    @Test
    void unauthorizedException_returns401WithGenericBody() throws Exception {
        mockMvc.perform(get("/test-exceptions/unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_token"));
    }

    @Test
    void unauthorizedException_doesNotLeakInternalDetails() throws Exception {
        mockMvc.perform(get("/test-exceptions/unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(not(containsString("reused"))))
                .andExpect(content().string(not(containsString("revoked"))))
                .andExpect(content().string(not(containsString("expired"))));
    }

    @Test
    void illegalArgumentException_returns400WithGenericMessage() throws Exception {
        mockMvc.perform(get("/test-exceptions/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid request"));
    }

    @Test
    void illegalArgumentException_doesNotLeakExceptionMessage() throws Exception {
        mockMvc.perform(get("/test-exceptions/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(containsString("internal detail that must not leak"))));
    }

    @Test
    void runtimeException_returns500WithGenericMessage() throws Exception {
        mockMvc.perform(get("/test-exceptions/runtime"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal server error"));
    }

    @Test
    void runtimeException_doesNotLeakExceptionMessage() throws Exception {
        mockMvc.perform(get("/test-exceptions/runtime"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(not(containsString("internal server detail"))));
    }

    @Test
    void methodArgumentNotValidException_returns400WithErrorsField() throws Exception {
        mockMvc.perform(post("/test-exceptions/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void methodArgumentNotValidException_errorsFieldContainsFieldName() throws Exception {
        mockMvc.perform(post("/test-exceptions/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }
}
