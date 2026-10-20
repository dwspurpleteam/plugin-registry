package ru.itmo.pluginservice.simplepluginregistry.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.itmo.pluginservice.simplepluginregistry.dto.PagedResponse;
import ru.itmo.pluginservice.simplepluginregistry.dto.PluginResponse;
import ru.itmo.pluginservice.simplepluginregistry.entity.PluginEntity;
import ru.itmo.pluginservice.simplepluginregistry.entity.PluginVersionEntity;
import ru.itmo.pluginservice.simplepluginregistry.entity.PluginVersionId;
import ru.itmo.pluginservice.simplepluginregistry.exception.ResourceNotFoundException;
import ru.itmo.pluginservice.simplepluginregistry.repository.PluginRepository;
import ru.itmo.pluginservice.simplepluginregistry.repository.PluginVersionRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PluginRegistryServiceTest {

    @Mock
    private PluginRepository pluginRepository;

    @Mock
    private PluginVersionRepository versionRepository;

    private PluginRegistryService service;

    @BeforeEach
    void setUp() {
        service = new PluginRegistryService(pluginRepository, versionRepository);
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
    void returnsManifestWithoutChangingItsStructure() throws Exception {
        PluginEntity plugin = new PluginEntity("plugin-1", "Test plugin", "Description");
        JsonNode manifest = new ObjectMapper().readTree("""
                {"manifest-version":"0.1","plugin-id":"plugin-1"}
                """);
        PluginVersionId id = new PluginVersionId("plugin-1", "1.0.0");
        when(versionRepository.findDetailedById(eq(id)))
                .thenReturn(Optional.of(new PluginVersionEntity(id, plugin, manifest)));

        assertThat(service.getManifest("plugin-1", "1.0.0")).isEqualTo(manifest);
    }

    @Test
    void reportsMissingVersion() {
        when(versionRepository.findDetailedById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getVersion("plugin-1", "9.9.9"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("9.9.9")
                .hasMessageContaining("plugin-1");
    }
}
