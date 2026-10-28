package ru.itmo.pluginservice.simplepluginregistry.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreatePluginVersionRequest(
        @NotBlank @Size(max = 50) String manifestVersion,
        @NotBlank @Size(max = 100) String pluginVersion,
        @NotNull @Valid Frontend frontend,
        @NotNull @Valid Backend backend
) {
    public record Frontend(
            @NotBlank @Size(max = 100) String sdkVersion,
            @NotBlank @Size(max = 2_048) String urlWeb
    ) {
    }

    public record Backend(
            @NotBlank @Size(max = 100) String sdkVersion,
            @NotBlank @Size(max = 2_048) String healthUrl,
            @NotNull @Valid Events events,
            @NotNull @Valid Media media
    ) {
    }

    public record Events(
            @NotBlank @Size(max = 2_048) String eventUrl,
            @NotNull List<@NotBlank String> availableEventTypes,
            @NotNull List<@NotBlank String> availableEventGroups
    ) {
    }

    public record Media(
            @NotNull List<@NotNull MediaType> input,
            @NotNull List<@NotNull MediaType> output
    ) {
    }

    public enum MediaType {
        AUDIO,
        VIDEO,
        SCREEN_SHARE,
        SCREEN_SHARE_AUDIO
    }
}
