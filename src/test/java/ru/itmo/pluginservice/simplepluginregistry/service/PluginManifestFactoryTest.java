package ru.itmo.pluginservice.simplepluginregistry.service;

import org.junit.jupiter.api.Test;
import ru.itmo.pluginservice.simplepluginregistry.dto.CreatePluginVersionRequest;
import ru.itmo.pluginservice.simplepluginregistry.entity.PluginEntity;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class PluginManifestFactoryTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PluginManifestFactory factory = new PluginManifestFactory();

    @Test
    void returnsAnUnmodifiedCopyIncludingBothEventListsAndUnknownFields() throws Exception {
        JsonNode source = objectMapper.readTree("""
                {"manifest-version":"1.1","plugin-id":"plugin-1","plugin-version":"1.0.0",
                 "name":"Test plugin","future-field":{"kept":true},"backend":{"events":{
                 "available-event-types":["participant.joined"],"available-event-groups":["participant"]}}}
                """);
        JsonNode result = factory.create(new PluginEntity("plugin-1", "Test plugin", null),
                new CreatePluginVersionRequest(source));
        assertThat(result).isEqualTo(source).isNotSameAs(source);
        assertThat(result.at("/backend/events/available-event-types").isArray()).isTrue();
        assertThat(result.at("/backend/events/available-event-groups").isArray()).isTrue();
        assertThat(result.at("/future-field/kept").asBoolean()).isTrue();
    }
}
