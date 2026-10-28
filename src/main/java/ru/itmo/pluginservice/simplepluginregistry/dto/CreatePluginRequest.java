package ru.itmo.pluginservice.simplepluginregistry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePluginRequest(
        @NotBlank @Size(max = 255) String id,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 10_000) String description
) {
}
