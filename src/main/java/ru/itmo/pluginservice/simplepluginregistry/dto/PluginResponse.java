package ru.itmo.pluginservice.simplepluginregistry.dto;

public record PluginResponse(
        String id,
        String name,
        String description
) {
}
