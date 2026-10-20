package ru.itmo.pluginservice.simplepluginregistry.dto;

public record PluginVersionResponse(
        String pluginId,
        String version,
        String name,
        String description,
        String frontendSdkVersion,
        String backendSdkVersion
) {
}
