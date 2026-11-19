package ru.itmo.pluginservice.simplepluginregistry.service;

import org.springframework.stereotype.Component;
import ru.itmo.pluginservice.simplepluginregistry.dto.CreatePluginVersionRequest;
import ru.itmo.pluginservice.simplepluginregistry.entity.PluginEntity;
import tools.jackson.databind.JsonNode;

@Component
public class PluginManifestFactory {

    public JsonNode create(PluginEntity plugin, CreatePluginVersionRequest request) {
        return request.manifest();
    }
}
