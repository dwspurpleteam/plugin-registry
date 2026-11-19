package ru.itmo.pluginservice.simplepluginregistry.controller;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.pluginservice.simplepluginregistry.dto.ManifestSchemaVersionsResponse;
import ru.itmo.pluginservice.simplepluginregistry.exception.ResourceNotFoundException;
import ru.itmo.pluginservice.simplepluginregistry.service.ManifestSchemaRegistry;

@RestController
@RequestMapping("/v1/manifest-schemas")
public class ManifestSchemaController {
    private static final MediaType JSON_SCHEMA_MEDIA_TYPE = MediaType.parseMediaType("application/schema+json");
    private final ManifestSchemaRegistry registry;

    public ManifestSchemaController(ManifestSchemaRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public ManifestSchemaVersionsResponse getManifestSchemas() {
        return new ManifestSchemaVersionsResponse(registry.supportedVersions());
    }

    @GetMapping(value = "/{version}", produces = "application/schema+json")
    public ResponseEntity<ByteArrayResource> getManifestSchema(@PathVariable String version) {
        ManifestSchemaRegistry.RegisteredSchema schema = registry.get(version);
        if (schema == null) throw new ResourceNotFoundException("Manifest schema '" + version + "' was not found");
        return ResponseEntity.ok().contentType(JSON_SCHEMA_MEDIA_TYPE).body(new ByteArrayResource(schema.source()));
    }
}
