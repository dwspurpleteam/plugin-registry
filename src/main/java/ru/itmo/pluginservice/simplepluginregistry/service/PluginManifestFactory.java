package ru.itmo.pluginservice.simplepluginregistry.service;

import org.springframework.stereotype.Component;
import ru.itmo.pluginservice.simplepluginregistry.dto.CreatePluginVersionRequest;
import ru.itmo.pluginservice.simplepluginregistry.entity.PluginEntity;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;

@Component
public class PluginManifestFactory {

    private final ObjectMapper objectMapper;

    public PluginManifestFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode create(PluginEntity plugin, CreatePluginVersionRequest request) {
        ObjectNode manifest = objectMapper.createObjectNode();
        manifest.put("manifest-version", request.manifestVersion());
        manifest.put("plugin-id", plugin.getId());
        manifest.put("plugin-version", request.pluginVersion());
        manifest.put("name", plugin.getName());
        if (plugin.getDescription() == null) {
            manifest.putNull("description");
        } else {
            manifest.put("description", plugin.getDescription());
        }

        ObjectNode frontend = manifest.putObject("frontend");
        frontend.put("sdk-version", request.frontend().sdkVersion());
        frontend.put("url-web", request.frontend().urlWeb());

        ObjectNode backend = manifest.putObject("backend");
        backend.put("sdk-version", request.backend().sdkVersion());
        backend.put("health-url", request.backend().healthUrl());

        ObjectNode events = backend.putObject("events");
        events.put("event-url", request.backend().events().eventUrl());
        if (!request.backend().events().availableEventTypes().isEmpty()) {
            putStrings(events, "available-event-types", request.backend().events().availableEventTypes());
        } else if (!request.backend().events().availableEventGroups().isEmpty()) {
            putStrings(events, "available-event-groups", request.backend().events().availableEventGroups());
        }

        ObjectNode media = backend.putObject("media");
        putMediaTypes(media, "input", request.backend().media().input());
        putMediaTypes(media, "output", request.backend().media().output());

        return manifest;
    }

    private void putStrings(ObjectNode parent, String fieldName, List<String> values) {
        ArrayNode array = parent.putArray(fieldName);
        values.forEach(array::add);
    }

    private void putMediaTypes(
            ObjectNode parent,
            String fieldName,
            List<CreatePluginVersionRequest.MediaType> values
    ) {
        ArrayNode array = parent.putArray(fieldName);
        values.stream()
                .map(Enum::name)
                .forEach(array::add);
    }
}
