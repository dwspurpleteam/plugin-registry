package ru.itmo.pluginservice.simplepluginregistry.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.pluginservice.simplepluginregistry.dto.PagedResponse;
import ru.itmo.pluginservice.simplepluginregistry.dto.PluginResponse;
import ru.itmo.pluginservice.simplepluginregistry.dto.PluginVersionResponse;
import ru.itmo.pluginservice.simplepluginregistry.entity.PluginEntity;
import ru.itmo.pluginservice.simplepluginregistry.entity.PluginVersionEntity;
import ru.itmo.pluginservice.simplepluginregistry.entity.PluginVersionId;
import ru.itmo.pluginservice.simplepluginregistry.exception.ResourceNotFoundException;
import ru.itmo.pluginservice.simplepluginregistry.repository.PluginRepository;
import ru.itmo.pluginservice.simplepluginregistry.repository.PluginVersionRepository;

import java.util.List;
import tools.jackson.databind.JsonNode;

@Service
@Transactional(readOnly = true)
public class PluginRegistryService {

    private final PluginRepository pluginRepository;
    private final PluginVersionRepository versionRepository;

    public PluginRegistryService(
            PluginRepository pluginRepository,
            PluginVersionRepository versionRepository
    ) {
        this.pluginRepository = pluginRepository;
        this.versionRepository = versionRepository;
    }

    public PagedResponse<PluginResponse> getPlugins(int page, int limit) {
        Page<PluginEntity> result = pluginRepository.findAll(
                PageRequest.of(page - 1, limit, Sort.by("id").ascending())
        );
        List<PluginResponse> items = result.getContent().stream()
                .map(this::toPluginResponse)
                .toList();
        return PagedResponse.from(result, items);
    }

    public PagedResponse<PluginVersionResponse> getVersions(String pluginId, int page, int limit) {
        PluginEntity plugin = findPlugin(pluginId);
        Page<PluginVersionEntity> result = versionRepository.findAllByIdPluginId(
                pluginId,
                PageRequest.of(page - 1, limit, Sort.by("id.version").ascending())
        );
        List<PluginVersionResponse> items = result.getContent().stream()
                .map(version -> toVersionResponse(version, plugin))
                .toList();
        return PagedResponse.from(result, items);
    }

    public PluginVersionResponse getVersion(String pluginId, String version) {
        PluginVersionEntity entity = findVersion(pluginId, version);
        return toVersionResponse(entity, entity.getPlugin());
    }

    public JsonNode getManifest(String pluginId, String version) {
        return findVersion(pluginId, version).getManifest();
    }

    private PluginEntity findPlugin(String pluginId) {
        return pluginRepository.findById(pluginId)
                .orElseThrow(() -> new ResourceNotFoundException("Plugin '%s' was not found".formatted(pluginId)));
    }

    private PluginVersionEntity findVersion(String pluginId, String version) {
        return versionRepository.findDetailedById(new PluginVersionId(pluginId, version))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Version '%s' of plugin '%s' was not found".formatted(version, pluginId)
                ));
    }

    private PluginResponse toPluginResponse(PluginEntity plugin) {
        return new PluginResponse(plugin.getId(), plugin.getName(), plugin.getDescription());
    }

    private PluginVersionResponse toVersionResponse(PluginVersionEntity version, PluginEntity plugin) {
        return new PluginVersionResponse(
                version.getId().getPluginId(),
                version.getId().getVersion(),
                plugin.getName(),
                plugin.getDescription(),
                version.getFrontendSdkVersion(),
                version.getBackendSdkVersion()
        );
    }
}
