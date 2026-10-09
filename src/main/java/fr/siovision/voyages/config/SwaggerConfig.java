package fr.siovision.voyages.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.ObjectSchema;
import tools.jackson.databind.JsonNode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public ModelConverter jacksonNodeSchema() {
        return (type, context, chain) -> {
            // Swagger uses Jackson 2 and cannot introspect Jackson 3 tree nodes.
            if (JsonNode.class.isAssignableFrom(Json.mapper().constructType(type.getType()).getRawClass())) {
                return new ObjectSchema();
            }
            return chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
        };
    }

    @Bean
    public OpenAPI apiDoc() {
        return new OpenAPI()
                .info(new Info().title("API Voyages").version("v1"))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new Components().addSecuritySchemes("BearerAuth",
                        new SecurityScheme()
                                .name("BearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
