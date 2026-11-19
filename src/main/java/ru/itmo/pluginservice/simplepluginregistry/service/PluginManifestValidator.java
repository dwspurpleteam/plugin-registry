package ru.itmo.pluginservice.simplepluginregistry.service;

import com.networknt.schema.Error;
import com.networknt.schema.ValidationLimitExceededException;
import org.springframework.stereotype.Component;
import ru.itmo.pluginservice.simplepluginregistry.exception.ManifestValidationException;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

@Component
public class PluginManifestValidator {
    private final ManifestSchemaRegistry schemaRegistry;

    public PluginManifestValidator(ManifestSchemaRegistry schemaRegistry) {
        this.schemaRegistry = schemaRegistry;
    }

    public void validate(JsonNode manifest) {
        if (manifest == null || !manifest.isObject()) {
            throw new ManifestValidationException(List.of("$: manifest must be a JSON object"));
        }
        JsonNode versionNode = manifest.get("manifest-version");
        if (versionNode == null || !versionNode.isTextual() || versionNode.asString().isBlank()) {
            throw new ManifestValidationException(List.of("manifest-version: must be a non-blank string"));
        }
        String version = versionNode.asString();
        ManifestSchemaRegistry.RegisteredSchema registered = schemaRegistry.get(version);
        if (registered == null) {
            throw new ManifestValidationException(List.of("manifest-version: unsupported version '" + version
                    + "'; supported versions: " + schemaRegistry.supportedVersions()));
        }
        List<Error> schemaErrors;
        try {
            schemaErrors = registered.compiled().validate(manifest, executionContext ->
                    executionContext.executionConfig(builder -> builder.formatAssertionsEnabled(true)
                            .maxEvaluationDepth(64).maxEvaluationSteps(10_000)));
        } catch (ValidationLimitExceededException exception) {
            throw new ManifestValidationException(List.of("$: manifest is too complex to validate"));
        }
        List<String> errors = new ArrayList<>();
        schemaErrors.stream().map(this::formatSchemaError).distinct().limit(20).forEach(errors::add);
        if (!errors.isEmpty()) throw new ManifestValidationException(errors);
    }

    private String formatSchemaError(Error error) {
        String path = jsonPointerToField(error.getInstanceLocation().toString());
        if (error.getProperty() != null) path = "$".equals(path) ? error.getProperty() : path + "." + error.getProperty();
        return path + ": " + error.getMessage();
    }

    private String jsonPointerToField(String pointer) {
        if (pointer == null || pointer.isBlank()) return "$";
        StringBuilder field = new StringBuilder();
        for (String token : pointer.substring(1).split("/")) {
            String decoded = token.replace("~1", "/").replace("~0", "~");
            if (decoded.chars().allMatch(Character::isDigit)) field.append('[').append(decoded).append(']');
            else {
                if (!field.isEmpty()) field.append('.');
                field.append(decoded);
            }
        }
        return field.toString();
    }
}
