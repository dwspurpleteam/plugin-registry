package ru.itmo.pluginservice.simplepluginregistry.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.pluginservice.simplepluginregistry.dto.CreatePluginRequest;
import ru.itmo.pluginservice.simplepluginregistry.dto.CreatePluginVersionRequest;
import ru.itmo.pluginservice.simplepluginregistry.dto.PagedResponse;
import ru.itmo.pluginservice.simplepluginregistry.dto.PluginResponse;
import ru.itmo.pluginservice.simplepluginregistry.dto.PluginVersionResponse;
import ru.itmo.pluginservice.simplepluginregistry.service.PluginRegistryService;
import tools.jackson.databind.JsonNode;

@Validated
@RestController
@RequestMapping("/v1/plugins")
public class PluginRegistryController {

    private final PluginRegistryService registryService;

    public PluginRegistryController(PluginRegistryService registryService) {
        this.registryService = registryService;
    }

    @GetMapping
    public PagedResponse<PluginResponse> getPlugins(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int limit
    ) {
        return registryService.getPlugins(page, limit);
    }

    @GetMapping("/{pluginId}/versions")
    public PagedResponse<PluginVersionResponse> getVersions(
            @PathVariable String pluginId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int limit
    ) {
        return registryService.getVersions(pluginId, page, limit);
    }

    @GetMapping("/{pluginId}/versions/{version}")
    public PluginVersionResponse getVersion(
            @PathVariable String pluginId,
            @PathVariable String version
    ) {
        return registryService.getVersion(pluginId, version);
    }

    @GetMapping("/{pluginId}/versions/{version}/manifest")
    public JsonNode getManifest(
            @PathVariable String pluginId,
            @PathVariable String version
    ) {
        return registryService.getManifest(pluginId, version);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PluginResponse createPlugin(@Valid @RequestBody CreatePluginRequest request) {
        return registryService.createPlugin(request);
    }

    @PostMapping("/{pluginId}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    public PluginVersionResponse createVersion(
            @PathVariable String pluginId,
            @Valid @RequestBody CreatePluginVersionRequest request
    ) {
        return registryService.createVersion(pluginId, request);
    }
}
