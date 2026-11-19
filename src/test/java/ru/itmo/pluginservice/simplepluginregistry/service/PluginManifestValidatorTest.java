package ru.itmo.pluginservice.simplepluginregistry.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import ru.itmo.pluginservice.simplepluginregistry.exception.ManifestValidationException;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PluginManifestValidatorTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void validatesSchema12WithoutKnowingItsFields() throws Exception {
        ManifestSchemaRegistry registry = new ManifestSchemaRegistry(objectMapper,
                new PathMatchingResourcePatternResolver(), "classpath*:test-plugin-manifest/*.schema.json");
        new PluginManifestValidator(registry).validate(objectMapper.readTree("""
                {"manifest-version":"1.2","plugin-id":"p","plugin-version":"2.0","name":"P",
                 "future-capability":{"mode":"schema-only","new-field":42}}
                """));
    }

    @Test
    void rejectsMissingWrongTypeAndUnknownManifestVersions() throws Exception {
        PluginManifestValidator validator = new PluginManifestValidator(new ManifestSchemaRegistry(objectMapper));
        assertThatThrownBy(() -> validator.validate(objectMapper.readTree("{}")))
                .isInstanceOf(ManifestValidationException.class).hasMessageContaining("manifest-version");
        assertThatThrownBy(() -> validator.validate(objectMapper.readTree("{\"manifest-version\":11}")))
                .isInstanceOf(ManifestValidationException.class).hasMessageContaining("non-blank string");
        assertThatThrownBy(() -> validator.validate(objectMapper.readTree("{\"manifest-version\":\"9.9\"}")))
                .isInstanceOf(ManifestValidationException.class).hasMessageContaining("supported versions");
    }
}
