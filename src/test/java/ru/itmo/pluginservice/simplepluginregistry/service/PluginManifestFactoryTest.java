package ru.itmo.pluginservice.simplepluginregistry.service;

import org.junit.jupiter.api.Test;
import ru.itmo.pluginservice.simplepluginregistry.dto.CreatePluginVersionRequest;
import ru.itmo.pluginservice.simplepluginregistry.entity.PluginEntity;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PluginManifestFactoryTest {

    private final PluginManifestFactory factory = new PluginManifestFactory(new ObjectMapper());

    @Test
    void buildsManifestWithSpecificationFieldNames() {
        PluginEntity plugin = new PluginEntity("plugin-1", "Test plugin", "Description");
        CreatePluginVersionRequest request = request(List.of(), List.of("participant", "media"));

        JsonNode manifest = factory.create(plugin, request);

        assertThat(manifest.get("manifest-version").asString()).isEqualTo("0.1");
        assertThat(manifest.get("plugin-id").asString()).isEqualTo("plugin-1");
        assertThat(manifest.get("plugin-version").asString()).isEqualTo("1.0.0");
        assertThat(manifest.get("frontend").get("sdk-version").asString()).isEqualTo("1.1.0");
        assertThat(manifest.get("backend").get("sdk-version").asString()).isEqualTo("2.1.0");
        assertThat(manifest.get("backend").get("events").has("available-event-groups")).isTrue();
        assertThat(manifest.get("backend").get("media").get("input").get(0).asString())
                .isEqualTo("AUDIO");
    }

    @Test
    void storesEventTypesInsteadOfGroupsWhenBothAreProvided() {
        CreatePluginVersionRequest request = request(
                List.of("participant.joined"),
                List.of("participant")
        );

        JsonNode events = factory.create(
                new PluginEntity("plugin-1", "Test plugin", null),
                request
        ).get("backend").get("events");

        assertThat(events.has("available-event-types")).isTrue();
        assertThat(events.has("available-event-groups")).isFalse();
    }

    private CreatePluginVersionRequest request(List<String> eventTypes, List<String> eventGroups) {
        return new CreatePluginVersionRequest(
                "0.1",
                "1.0.0",
                new CreatePluginVersionRequest.Frontend("1.1.0", "https://example.com/ui"),
                new CreatePluginVersionRequest.Backend(
                        "2.1.0",
                        "https://example.com/health",
                        new CreatePluginVersionRequest.Events(
                                "https://example.com/events",
                                eventTypes,
                                eventGroups
                        ),
                        new CreatePluginVersionRequest.Media(
                                List.of(CreatePluginVersionRequest.MediaType.AUDIO),
                                List.of(CreatePluginVersionRequest.MediaType.VIDEO)
                        )
                )
        );
    }
}
