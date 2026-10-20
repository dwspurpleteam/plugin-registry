package ru.itmo.pluginservice.simplepluginregistry.controller;

import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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
    public ProblemDetail uploadPluginStub() {
        return notImplemented("Plugin upload is not implemented yet");
    }

    @PostMapping("/{pluginId}/versions")
    public ProblemDetail uploadVersionStub(@PathVariable String pluginId) {
        return notImplemented("Version upload for plugin '%s' is not implemented yet".formatted(pluginId));
    }

    private ProblemDetail notImplemented(String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_IMPLEMENTED, message);
        detail.setTitle("Not implemented");
        return detail;
    }
}
