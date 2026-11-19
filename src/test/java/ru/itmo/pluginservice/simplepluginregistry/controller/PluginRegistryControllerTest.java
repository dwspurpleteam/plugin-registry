package ru.itmo.pluginservice.simplepluginregistry.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.itmo.pluginservice.simplepluginregistry.entity.PluginEntity;
import ru.itmo.pluginservice.simplepluginregistry.entity.PluginVersionEntity;
import ru.itmo.pluginservice.simplepluginregistry.entity.PluginVersionId;
import ru.itmo.pluginservice.simplepluginregistry.exception.ApiExceptionHandler;
import ru.itmo.pluginservice.simplepluginregistry.repository.PluginRepository;
import ru.itmo.pluginservice.simplepluginregistry.repository.PluginVersionRepository;
import ru.itmo.pluginservice.simplepluginregistry.service.PluginManifestFactory;
import ru.itmo.pluginservice.simplepluginregistry.service.PluginManifestValidator;
import ru.itmo.pluginservice.simplepluginregistry.service.ManifestSchemaRegistry;
import ru.itmo.pluginservice.simplepluginregistry.service.PluginRegistryService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PluginRegistryControllerTest {

    @Mock
    private PluginRepository pluginRepository;

    @Mock
    private PluginVersionRepository versionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PluginRegistryService registryService = new PluginRegistryService(
                pluginRepository,
                versionRepository,
                new PluginManifestFactory(),
                new PluginManifestValidator(new ManifestSchemaRegistry(objectMapper))
        );
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PluginRegistryController(registryService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void createsPlugin() throws Exception {
        when(pluginRepository.save(any(PluginEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/v1/plugins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": "plugin-1",
                                  "name": "Test plugin",
                                  "description": "Description"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("plugin-1"));
    }

    @Test
    void publishesFullManifestVersion11WithoutLosingOptionalBlocks() throws Exception {
        ObjectNode manifest = fullManifest();

        publish(manifest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pluginId").value("plugin-1"))
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.frontendSdkVersion").value("1.2.3"))
                .andExpect(jsonPath("$.backendSdkVersion").value("4.5.6"));

        ArgumentCaptor<PluginVersionEntity> captor = ArgumentCaptor.forClass(PluginVersionEntity.class);
        verify(versionRepository).save(captor.capture());
        JsonNode saved = captor.getValue().getManifest();
        assertThat(saved.at("/backend/control-url").asString()).isEqualTo("https://example.com/control");
        assertThat(saved.at("/backend/initial-state/0").asString()).isEqualTo("participants");
        assertThat(saved.at("/backend/events/available-event-types/2").asString()).isEqualTo("call.ended");
        assertThat(saved.at("/backend/media/input/0").asString()).isEqualTo("AUDIO");
        assertThat(saved.at("/backend/results/keys/0").asString()).isEqualTo("attendance.summary");
        assertThat(saved.at("/backend/results/delivery-report-url").asString())
                .isEqualTo("https://example.com/deliveries");
    }

    @Test
    void publishesMinimalManifestWithoutSdkEventsOrMedia() throws Exception {
        publish(minimalManifest())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.frontendSdkVersion").doesNotExist())
                .andExpect(jsonPath("$.backendSdkVersion").doesNotExist());
    }

    @Test
    void preservesBothEventListsWithoutApplyingVersionSpecificPrecedence() throws Exception {
        ObjectNode manifest = minimalManifest();
        ObjectNode events = ((ObjectNode) manifest.get("backend")).putObject("events");
        events.putArray("available-event-types").add("participant.joined");
        events.putArray("available-event-groups").add("participant");

        publish(manifest).andExpect(status().isCreated());

        ArgumentCaptor<PluginVersionEntity> captor = ArgumentCaptor.forClass(PluginVersionEntity.class);
        verify(versionRepository).save(captor.capture());
        JsonNode savedEvents = captor.getValue().getManifest().at("/backend/events");
        assertThat(savedEvents.has("available-event-types")).isTrue();
        assertThat(savedEvents.has("available-event-groups")).isTrue();
    }

    @Test
    void rejectsMissingControlUrl() throws Exception {
        ObjectNode manifest = minimalManifest();
        ObjectNode backend = (ObjectNode) manifest.get("backend");
        backend.remove("control-url");

        publish(manifest)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("backend.control-url")));
    }

    @Test
    void rejectsLegacyEventUrl() throws Exception {
        ObjectNode manifest = minimalManifest();
        ObjectNode backend = (ObjectNode) manifest.get("backend");
        ObjectNode events = backend.putObject("events");
        events.put("event-url", "https://example.com/events");
        events.putArray("available-event-types").add("participant.joined");

        publish(manifest)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("event-url")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.1", "1.0"})
    void rejectsLegacyManifestVersions(String manifestVersion) throws Exception {
        ObjectNode manifest = minimalManifest();
        manifest.put("manifest-version", manifestVersion);

        publish(manifest)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("manifest-version")));
    }

    @Test
    void acceptsParticipantsInitialStateAndCallEnded() throws Exception {
        publish(fullManifest())
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsUnknownInitialState() throws Exception {
        ObjectNode manifest = minimalManifest();
        ((ObjectNode) manifest.get("backend")).putArray("initial-state").add("recording");

        publish(manifest)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("initial-state")));
    }

    @Test
    void rejectsEmptyResultKeys() throws Exception {
        ObjectNode manifest = minimalManifest();
        ((ObjectNode) manifest.get("backend")).putObject("results").putArray("keys");

        publish(manifest)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("results.keys")));
    }

    @Test
    void rejectsDuplicateOrBlankResultKeys() throws Exception {
        ObjectNode manifest = minimalManifest();
        ((ObjectNode) manifest.get("backend"))
                .putObject("results")
                .putArray("keys")
                .add("attendance.summary")
                .add("attendance.summary")
                .add(" ");

        publish(manifest)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("results.keys")));
    }

    @Test
    void rejectsDuplicateArrayItems() throws Exception {
        ObjectNode manifest = minimalManifest();
        ((ObjectNode) manifest.get("backend"))
                .putArray("initial-state")
                .add("participants")
                .add("participants");

        publish(manifest)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("initial-state")));
    }

    @Test
    void rejectsEmptyEventsBlock() throws Exception {
        ObjectNode manifest = minimalManifest();
        ((ObjectNode) manifest.get("backend")).putObject("events");

        publish(manifest)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("backend.events")));
    }

    @Test
    void rejectsUnknownAndDuplicateMediaTypes() throws Exception {
        ObjectNode manifest = minimalManifest();
        ObjectNode media = ((ObjectNode) manifest.get("backend")).putObject("media");
        media.putArray("input").add("AUDIO").add("AUDIO");
        media.putArray("output").add("DATA");

        publish(manifest)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("backend.media")));
    }

    @ParameterizedTest
    @CsvSource({
            "available-event-types,unknown.event",
            "available-event-groups,unknown-group"
    })
    void rejectsUnknownEventTypeOrGroup(String listName, String value) throws Exception {
        ObjectNode manifest = minimalManifest();
        ((ObjectNode) manifest.get("backend"))
                .putObject("events")
                .putArray(listName)
                .add(value);

        publish(manifest)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString(listName)));
    }

    @Test
    void rejectsMalformedUrl() throws Exception {
        ObjectNode manifest = minimalManifest();
        ((ObjectNode) manifest.get("frontend")).put("url-web", "https://");

        publish(manifest)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("frontend.url-web")));
    }

    @Test
    void rejectsInvalidPort() throws Exception {
        ObjectNode manifest = minimalManifest();
        ((ObjectNode) manifest.get("backend")).put("control-url", "https://example.com:70000/control");

        publish(manifest)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("backend.control-url")));
    }

    @Test
    void rejectsLocalServerToServerTarget() throws Exception {
        ObjectNode manifest = minimalManifest();
        ((ObjectNode) manifest.get("backend")).put("health-url", "https://127.0.0.1/health");

        publish(manifest)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("backend.health-url")));
    }

    @Test
    void rejectsUnknownManifestField() throws Exception {
        ObjectNode manifest = minimalManifest();
        manifest.put("activationId", "activation-1");

        publish(manifest)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("activationId")));
    }

    @Test
    void rejectsNullOptionalFieldInsteadOfSilentlyDroppingIt() throws Exception {
        ObjectNode manifest = minimalManifest();
        manifest.putNull("description");

        publish(manifest)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("description")));
    }

    @Test
    void rejectsPluginIdThatDiffersFromPath() throws Exception {
        ObjectNode manifest = minimalManifest();
        manifest.put("plugin-id", "another-plugin");

        publish(manifest)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("plugin-id")));
    }

    @Test
    void returnsConflictForRepeatedPluginVersion() throws Exception {
        ObjectNode manifest = minimalManifest();
        when(versionRepository.existsById(new PluginVersionId("plugin-1", "1.0.0"))).thenReturn(true);

        publish(manifest)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("already exists")));
    }

    @Test
    void returnsLegacyPublishedManifestUnchanged() throws Exception {
        PluginEntity plugin = new PluginEntity("plugin-1", "Test plugin", null);
        JsonNode legacy = objectMapper.readTree("""
                {
                  "manifest-version":"0.1",
                  "plugin-id":"plugin-1",
                  "plugin-version":"0.9.0",
                  "backend":{"events":{"event-url":"https://example.com/events"}}
                }
                """);
        PluginVersionId id = new PluginVersionId("plugin-1", "0.9.0");
        when(versionRepository.findDetailedById(id))
                .thenReturn(Optional.of(new PluginVersionEntity(id, plugin, legacy)));

        mockMvc.perform(get("/v1/plugins/plugin-1/versions/0.9.0/manifest"))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(legacy), JsonCompareMode.STRICT));
    }

    @Test
    void rejectsInvalidPluginRequest() throws Exception {
        mockMvc.perform(post("/v1/plugins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":"", "name":"", "description":null}
                                """))
                .andExpect(status().isBadRequest());
    }

    private ResultActions publish(ObjectNode manifest) throws Exception {
        String name = manifest.path("name").asString();
        JsonNode description = manifest.get("description");
        when(pluginRepository.findById("plugin-1"))
                .thenReturn(Optional.of(new PluginEntity("plugin-1", name,
                        description != null && description.isString() ? description.asString() : null)));
        return mockMvc.perform(post("/v1/plugins/plugin-1/versions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(manifest)));
    }

    private ObjectNode minimalManifest() throws Exception {
        return (ObjectNode) objectMapper.readTree("""
                {
                  "manifest-version": "1.1",
                  "plugin-id": "plugin-1",
                  "plugin-version": "1.0.0",
                  "name": "Submitted plugin name",
                  "frontend": {
                    "url-web": "https://example.com/ui"
                  },
                  "backend": {
                    "health-url": "https://example.com/health",
                    "control-url": "https://example.com/control"
                  }
                }
                """);
    }

    private ObjectNode fullManifest() throws Exception {
        return (ObjectNode) objectMapper.readTree("""
                {
                  "manifest-version": "1.1",
                  "plugin-id": "plugin-1",
                  "plugin-version": "1.0.0",
                  "name": "Submitted plugin name",
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
                      "available-event-types": [
                        "participant.joined",
                        "participant.left",
                        "call.ended"
                      ]
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
    }
}
