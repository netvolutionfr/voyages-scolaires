package fr.siovision.voyages.config;

import fr.siovision.voyages.infrastructure.dto.gdpr.GdprHealthFormDTO;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;

class SwaggerConfigTest {
    @Test
    void jsonTreesHaveAnObjectSchemaAndDtoFieldsRemainDocumented() {
        ModelConverters converters = new ModelConverters();
        converters.addConverter(new SwaggerConfig().jacksonNodeSchema());

        assertThat(converters.resolveAsResolvedSchema(new AnnotatedType(ObjectNode.class)).schema.getType())
                .isEqualTo("object");
        var schemas = converters.read(GdprHealthFormDTO.class);
        assertThat(schemas.get("GdprHealthFormDTO").getProperties())
                .containsKeys("signedAt", "validUntil", "payload");
        assertThat(schemas).doesNotContainKey("ObjectNode");
    }
}
