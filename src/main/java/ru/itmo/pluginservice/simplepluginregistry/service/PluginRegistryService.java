package ru.itmo.pluginservice.simplepluginregistry.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

import java.util.List;
import tools.jackson.databind.JsonNode;

@Service
@Transactional(readOnly = true)
public class PluginRegistryService {

    private final PluginRepository pluginRepository;
    private final PluginVersionRepository versionRepository;
    private final PluginManifestFactory manifestFactory;
    private final PluginManifestValidator manifestValidator;

    public PluginRegistryService(
            PluginRepository pluginRepository,
            PluginVersionRepository versionRepository,
            PluginManifestFactory manifestFactory,
            PluginManifestValidator manifestValidator
    ) {
        this.pluginRepository = pluginRepository;
        this.versionRepository = versionRepository;
        this.manifestFactory = manifestFactory;
        this.manifestValidator = manifestValidator;
    }

    @Transactional
    public PluginResponse createPlugin(CreatePluginRequest request) {
        if (pluginRepository.existsById(request.id())) {
            throw new ResourceConflictException("Plugin '%s' already exists".formatted(request.id()));
        }

        PluginEntity plugin = pluginRepository.save(
                new PluginEntity(request.id(), request.name(), request.description())
        );
        return toPluginResponse(plugin);
    }

    @Transactional
    public PluginVersionResponse createVersion(
            String pluginId,
            CreatePluginVersionRequest request
    ) {
        PluginEntity plugin = findPlugin(pluginId);
        manifestValidator.validate(request.manifest());
        if (!pluginId.equals(request.pluginId())) {
            throw new ManifestValidationException(List.of(
                    "plugin-id: must match path parameter '" + pluginId + "'"
            ));
        }
        String pluginVersion = request.pluginVersion();
        PluginVersionId id = new PluginVersionId(pluginId, pluginVersion);
        if (versionRepository.existsById(id)) {
            throw new ResourceConflictException(
                    "Version '%s' of plugin '%s' already exists".formatted(pluginVersion, pluginId)
            );
        }
        validateRegistryMetadata(plugin, request.manifest());

        JsonNode manifest = manifestFactory.create(plugin, request);
        versionRepository.save(new PluginVersionEntity(id, plugin, manifest));

        return new PluginVersionResponse(
                pluginId,
                pluginVersion,
                plugin.getName(),
                plugin.getDescription(),
                optionalText(manifest, "/frontend/sdk-version"),
                optionalText(manifest, "/backend/sdk-version")
        );
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

    private String optionalText(JsonNode node, String pointer) {
        JsonNode value = node.at(pointer);
        return value.isString() ? value.asString() : null;
    }

    private void validateRegistryMetadata(PluginEntity plugin, JsonNode manifest) {
        List<String> errors = new java.util.ArrayList<>();
        JsonNode name = manifest.get("name");
        if (name != null && name.isString() && !plugin.getName().equals(name.asString())) {
            errors.add("name: must match registry metadata '" + plugin.getName() + "'");
        }
        JsonNode description = manifest.get("description");
        String expectedDescription = plugin.getDescription();
        if (description != null && description.isString()
                && !java.util.Objects.equals(expectedDescription, description.asString())) {
            errors.add("description: must match registry metadata");
        }
        if (description == null && expectedDescription != null && !expectedDescription.isBlank()) {
            errors.add("description: must match registry metadata");
        }
        if (!errors.isEmpty()) throw new ManifestValidationException(errors);
    }
}
