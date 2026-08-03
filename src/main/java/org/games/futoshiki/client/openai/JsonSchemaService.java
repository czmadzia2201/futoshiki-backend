package org.games.futoshiki.client.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.victools.jsonschema.generator.*;
import org.springframework.stereotype.Component;

@Component
public class JsonSchemaService {

    private final SchemaGenerator generator;

    public JsonSchemaService() {
        SchemaGeneratorConfigBuilder configBuilder =
                new SchemaGeneratorConfigBuilder(
                        SchemaVersion.DRAFT_2020_12,
                        OptionPreset.PLAIN_JSON
                )
                        .with(Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT);

        configBuilder.forFields()
                .withRequiredCheck(field -> true);

        SchemaGeneratorConfig config = configBuilder.build();
        this.generator = new SchemaGenerator(config);
    }

    public JsonNode generate(Class<?> clazz) {
        return generator.generateSchema(clazz);
    }

}
