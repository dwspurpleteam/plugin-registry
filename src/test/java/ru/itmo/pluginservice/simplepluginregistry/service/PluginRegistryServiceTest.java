package ru.itmo.pluginservice.simplepluginregistry.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.itmo.pluginservice.simplepluginregistry.dto.CreatePluginRequest;
import ru.itmo.pluginservice.simplepluginregistry.dto.CreatePluginVersionRequest;
import ru.itmo.pluginservice.simplepluginregistry.dto.PagedResponse;
import ru.itmo.pluginservice.simplepluginregistry.dto.PluginResponse;
import ru.itmo.pluginservice.simplepluginregistry.dto.PluginVersionResponse;
import ru.itmo.pluginservice.simplepluginregistry.entity.PluginEntity;
import ru.itmo.pluginservice.simplepluginregistry.entity.PluginVersionEntity;
import ru.itmo.pluginservice.simplepluginregistry.entity.PluginVersionId;
import ru.itmo.pluginservice.simplepluginregistry.exception.ManifestValidationException;
import ru.itmo.pluginservice.simplepluginregistry.exception.ResourceConflictException;
import ru.itmo.pluginservice.simplepluginregistry.exception.ResourceNotFoundException;
import ru.itmo.pluginservice.simplepluginregistry.repository.PluginRepository;
import ru.itmo.pluginservice.simplepluginregistry.repository.PluginVersionRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PluginRegistryServiceTest {

    @Mock
    private PluginRepository pluginRepository;

    @Mock
    private PluginVersionRepository versionRepository;

    @Mock
    private PluginManifestFactory manifestFactory;

    @Mock
    private PluginManifestValidator manifestValidator;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private PluginRegistryService service;

    @BeforeEach
    void setUp() {
        service = new PluginRegistryService(
                pluginRepository,
                versionRepository,
                manifestFactory,
                manifestValidator
        );
    }

    @Test
    void createsPluginMetadata() {
        CreatePluginRequest request = new CreatePluginRequest("plugin-1", "Test plugin", "Description");
        when(pluginRepository.save(any(PluginEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PluginResponse response = service.createPlugin(request);

        assertThat(response).isEqualTo(new PluginResponse("plugin-1", "Test plugin", "Description"));
        verify(pluginRepository).save(any(PluginEntity.class));
    }

    @Test
    void rejectsDuplicatePlugin() {
        when(pluginRepository.existsById("plugin-1")).thenReturn(true);

        assertThatThrownBy(() -> service.createPlugin(
                new CreatePluginRequest("plugin-1", "Test plugin", null)
        ))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("plugin-1");
    }

    @Test
    void validatesSubmittedManifestAndSavesItWithoutTransformation() throws Exception {
        PluginEntity plugin = new PluginEntity("plugin-1", "Test plugin", "Description");
        CreatePluginVersionRequest request = createVersionRequest();
        JsonNode submitted = request.manifest();
        JsonNode manifest = submitted;
        when(pluginRepository.findById("plugin-1")).thenReturn(Optional.of(plugin));
        when(manifestFactory.create(plugin, request)).thenReturn(manifest);

        PluginVersionResponse response = service.createVersion("plugin-1", request);

        assertThat(response).isEqualTo(new PluginVersionResponse(
                "plugin-1", "1.0.0", "Test plugin", "Description", null, null
        ));
        InOrder order = inOrder(manifestValidator, manifestFactory, versionRepository);
        order.verify(manifestValidator).validate(submitted);
        order.verify(manifestFactory).create(plugin, request);
        order.verify(versionRepository).save(any(PluginVersionEntity.class));
    }

    @Test
    void rejectsPluginIdThatDoesNotMatchPublicationPath() throws Exception {
        PluginEntity plugin = new PluginEntity("plugin-1", "Test plugin", null);
        CreatePluginVersionRequest request = request(objectMapper.readTree("""
                {
                  "manifest-version":"1.1",
                  "plugin-id":"another-plugin",
                  "plugin-version":"1.0.0",
                  "name":"Test plugin",
                  "description":"Description",
                  "frontend":{"url-web":"https://example.com/ui"},
                  "backend":{
                    "health-url":"https://example.com/health",
                    "control-url":"https://example.com/control"
                  }
                }
                """));
        when(pluginRepository.findById("plugin-1")).thenReturn(Optional.of(plugin));

        assertThatThrownBy(() -> service.createVersion("plugin-1", request))
                .isInstanceOf(ManifestValidationException.class)
                .hasMessageContaining("plugin-id");
        verify(versionRepository, never()).save(any());
    }

    @Test
    void rejectsDuplicatePluginVersion() throws Exception {
        PluginEntity plugin = new PluginEntity("plugin-1", "Test plugin", null);
        CreatePluginVersionRequest request = createVersionRequest();
        when(pluginRepository.findById("plugin-1")).thenReturn(Optional.of(plugin));
        when(versionRepository.existsById(new PluginVersionId("plugin-1", "1.0.0"))).thenReturn(true);

        assertThatThrownBy(() -> service.createVersion("plugin-1", request))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("1.0.0");
        verify(versionRepository, never()).save(any());
    }

    @Test
    void returnsOneBasedPluginPage() {
        PluginEntity plugin = new PluginEntity("plugin-1", "Test plugin", "Description");
        when(pluginRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(plugin), PageRequest.of(0, 20), 1));

        PagedResponse<PluginResponse> response = service.getPlugins(1, 20);

        assertThat(response.page()).isEqualTo(1);
        assertThat(response.limit()).isEqualTo(20);
        assertThat(response.totalItems()).isEqualTo(1);
        assertThat(response.items()).containsExactly(
                new PluginResponse("plugin-1", "Test plugin", "Description")
        );
    }

    @Test
    void returnsPreviouslyPublishedLegacyManifestWithoutConversion() throws Exception {
        PluginEntity plugin = new PluginEntity("plugin-1", "Test plugin", "Description");
        JsonNode legacyManifest = objectMapper.readTree("""
                {
                  "manifest-version":"0.1",
                  "plugin-id":"plugin-1",
                  "plugin-version":"0.9.0",
                  "backend":{"events":{"event-url":"https://example.com/events"}}
                }
                """);
        PluginVersionId id = new PluginVersionId("plugin-1", "0.9.0");
        when(versionRepository.findDetailedById(eq(id)))
                .thenReturn(Optional.of(new PluginVersionEntity(id, plugin, legacyManifest)));

        assertThat(service.getManifest("plugin-1", "0.9.0")).isEqualTo(legacyManifest);
        verify(manifestValidator, never()).validate(any());
    }

    @Test
    void returnsNullSdkMetadataWhenGeneratedColumnsAreNull() throws Exception {
        PluginEntity plugin = new PluginEntity("plugin-1", "Test plugin", null);
        JsonNode manifestWithoutSdk = objectMapper.readTree("""
                {
                  "manifest-version":"1.1",
                  "plugin-id":"plugin-1",
                  "plugin-version":"1.0.0",
                  "name":"Test plugin",
                  "description":"Description",
                  "frontend":{"url-web":"https://example.com/ui"},
                  "backend":{
                    "health-url":"https://example.com/health",
                    "control-url":"https://example.com/control"
                  }
                }
                """);
        PluginVersionId id = new PluginVersionId("plugin-1", "1.0.0");
        when(versionRepository.findDetailedById(id))
                .thenReturn(Optional.of(new PluginVersionEntity(id, plugin, manifestWithoutSdk)));

        PluginVersionResponse response = service.getVersion("plugin-1", "1.0.0");

        assertThat(response.frontendSdkVersion()).isNull();
        assertThat(response.backendSdkVersion()).isNull();
    }

    @Test
    void reportsMissingVersion() {
        when(versionRepository.findDetailedById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getVersion("plugin-1", "9.9.9"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("9.9.9")
                .hasMessageContaining("plugin-1");
    }

    private CreatePluginVersionRequest createVersionRequest() throws Exception {
        return request(objectMapper.readTree("""
                {
                  "manifest-version":"1.1",
                  "plugin-id":"plugin-1",
                  "plugin-version":"1.0.0",
                  "name":"Test plugin",
                  "description":"Description",
                  "frontend":{"url-web":"https://example.com/ui"},
                  "backend":{
                    "health-url":"https://example.com/health",
                    "control-url":"https://example.com/control"
                  }
                }
                """));
    }

    private CreatePluginVersionRequest request(JsonNode manifest) {
        return new CreatePluginVersionRequest(manifest);
    }
}
