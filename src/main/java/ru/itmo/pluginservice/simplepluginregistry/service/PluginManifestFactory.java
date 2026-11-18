package ru.itmo.pluginservice.simplepluginregistry.service;

import org.springframework.stereotype.Component;
import ru.itmo.pluginservice.simplepluginregistry.dto.CreatePluginVersionRequest;
import ru.itmo.pluginservice.simplepluginregistry.entity.PluginEntity;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

@Component
public class PluginManifestFactory {

    public JsonNode create(PluginEntity plugin, CreatePluginVersionRequest request) {
        ObjectNode manifest = (ObjectNode) request.manifest();

        manifest.put("plugin-id", plugin.getId());
        manifest.put("name", plugin.getName());
        if (plugin.getDescription() == null || plugin.getDescription().isBlank()) {
            manifest.remove("description");
        } else {
            manifest.put("description", plugin.getDescription());
        }

        retainEffectiveEventSubscription(manifest);
        return manifest;
    }

    private void retainEffectiveEventSubscription(ObjectNode manifest) {
        JsonNode eventsNode = manifest.at("/backend/events");
        if (eventsNode instanceof ObjectNode events
                && events.path("available-event-types").isArray()
                && !events.path("available-event-types").isEmpty()) {
            events.remove("available-event-groups");
        }
    }
}
