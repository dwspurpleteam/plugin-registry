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
    void buildsFullManifestAndKeepsAllVersion11Capabilities() throws Exception {
        PluginEntity plugin = new PluginEntity("plugin-1", "Registry name", "Registry description");
        CreatePluginVersionRequest request = request("""
                {
                  "manifest-version": "1.1",
                  "plugin-id": "plugin-1",
                  "plugin-version": "2.0.0",
                  "name": "Submitted name",
                  "description": "Submitted description",
                  "frontend": {
                    "url-web": "https://example.com/ui",
                    "sdk-version": "1.2.3"
                  },
                  "backend": {
                    "health-url": "https://example.com/health",
                    "control-url": "https://example.com/control",
                    "sdk-version": "4.5.6",
                    "initial-state": ["participants"],
                    "events": {
                      "available-event-types": ["participant.joined", "call.ended"]
                    },
                    "media": {
                      "input": ["AUDIO"],
                      "output": ["SCREEN_SHARE_AUDIO"]
                    },
                    "results": {
                      "keys": ["attendance.summary"],
                      "delivery-report-url": "https://example.com/deliveries"
                    }
                  }
                }
                """);

        JsonNode manifest = factory.create(plugin, request);

        assertThat(manifest.at("/manifest-version").asString()).isEqualTo("1.1");
        assertThat(manifest.at("/plugin-id").asString()).isEqualTo("plugin-1");
        assertThat(manifest.at("/plugin-version").asString()).isEqualTo("2.0.0");
        assertThat(manifest.at("/name").asString()).isEqualTo("Registry name");
        assertThat(manifest.at("/description").asString()).isEqualTo("Registry description");
        assertThat(manifest.at("/frontend/sdk-version").asString()).isEqualTo("1.2.3");
        assertThat(manifest.at("/backend/control-url").asString()).isEqualTo("https://example.com/control");
        assertThat(manifest.at("/backend/initial-state/0").asString()).isEqualTo("participants");
        assertThat(manifest.at("/backend/events/available-event-types/1").asString()).isEqualTo("call.ended");
        assertThat(manifest.at("/backend/media/output/0").asString()).isEqualTo("SCREEN_SHARE_AUDIO");
        assertThat(manifest.at("/backend/results/keys/0").asString()).isEqualTo("attendance.summary");
        assertThat(manifest.at("/backend/results/delivery-report-url").asString())
                .isEqualTo("https://example.com/deliveries");
    }

    @Test
    void keepsMinimalManifestMinimalAndOmitsMissingDescription() throws Exception {
        CreatePluginVersionRequest request = request("""
                {
                  "manifest-version": "1.1",
                  "plugin-id": "plugin-1",
                  "plugin-version": "1.0.0",
                  "name": "Submitted name",
                  "frontend": {"url-web": "https://example.com/ui"},
                  "backend": {
                    "health-url": "https://example.com/health",
                    "control-url": "https://example.com/control"
                  }
                }
                """);

        JsonNode manifest = factory.create(
                new PluginEntity("plugin-1", "Registry name", null),
                request
        );

        assertThat(manifest.has("description")).isFalse();
        assertThat(manifest.at("/frontend").has("sdk-version")).isFalse();
        assertThat(manifest.at("/backend").has("sdk-version")).isFalse();
        assertThat(manifest.at("/backend").has("events")).isFalse();
        assertThat(manifest.at("/backend").has("media")).isFalse();
        assertThat(manifest.at("/backend").has("initial-state")).isFalse();
        assertThat(manifest.at("/backend").has("results")).isFalse();
    }

    @Test
    void storesOnlyEffectiveEventTypesWhenBothEventListsAreProvided() throws Exception {
        CreatePluginVersionRequest request = request("""
                {
                  "manifest-version": "1.1",
                  "plugin-id": "plugin-1",
                  "plugin-version": "1.0.0",
                  "name": "Test plugin",
                  "frontend": {"url-web": "https://example.com/ui"},
                  "backend": {
                    "health-url": "https://example.com/health",
                    "control-url": "https://example.com/control",
                    "events": {
                      "available-event-types": ["participant.joined"],
                      "available-event-groups": ["participant"]
                    }
                  }
                }
                """);

        JsonNode events = factory.create(
                new PluginEntity("plugin-1", "Test plugin", null),
                request
        ).at("/backend/events");

        assertThat(events.has("available-event-types")).isTrue();
        assertThat(events.has("available-event-groups")).isFalse();
    }

    private CreatePluginVersionRequest request(String json) throws Exception {
        return new CreatePluginVersionRequest(objectMapper.readTree(json));
    }
}
