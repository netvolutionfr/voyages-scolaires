package fr.siovision.voyages.config;

import com.webauthn4j.converter.jackson.WebAuthnJSONModule;
import com.webauthn4j.converter.util.ObjectConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebAuthnJacksonConfig {
    @Bean
    public ObjectConverter webAuthnObjectConverter() {
        // Par défaut, il installe les modules nécessaires
        return new ObjectConverter();
    }

    @Bean
    public WebAuthnJSONModule webAuthnJSONModule(ObjectConverter objectConverter) {
        return new WebAuthnJSONModule(objectConverter);
    }
}
