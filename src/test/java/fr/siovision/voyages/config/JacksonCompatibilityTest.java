package fr.siovision.voyages.config;

import com.webauthn4j.data.PublicKeyCredentialUserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonCompatibilityTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
            .withUserConfiguration(WebAuthnJacksonConfig.class)
            .withPropertyValues("spring.jackson.use-jackson2-defaults=true");

    @Test
    void apiDatesKeepTheirIsoFormat() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            JsonMapper mapper = context.getBean(JsonMapper.class);
            String json = mapper.writeValueAsString(Map.of("createdAt", Instant.parse("2026-10-09T12:00:00Z")));
            assertThat(mapper.readTree(json).get("createdAt").asText()).isEqualTo("2026-10-09T12:00:00Z");
        });
    }

    @Test
    void webAuthnModuleKeepsUserHandlesInBase64Url() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            JsonMapper mapper = context.getBean(JsonMapper.class);
            var user = new PublicKeyCredentialUserEntity(new byte[]{(byte) 0xfb, (byte) 0xff}, "test@example.com", "Test");
            var json = mapper.readTree(mapper.writeValueAsString(user));
            assertThat(json.get("id").asText()).isEqualTo("-_8");
            assertThat(json.get("name").asText()).isEqualTo("test@example.com");
        });
    }
}
