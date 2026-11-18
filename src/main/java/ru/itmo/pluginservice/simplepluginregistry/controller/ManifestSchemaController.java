package ru.itmo.pluginservice.simplepluginregistry.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/manifest-schemas")
public class ManifestSchemaController {

    private static final Resource MANIFEST_SCHEMA_1_1 =
            new ClassPathResource("schema/plugin-manifest-v1.1.schema.json");
    private static final MediaType JSON_SCHEMA_MEDIA_TYPE =
            MediaType.parseMediaType("application/schema+json");

    @GetMapping(value = "/1.1", produces = "application/schema+json")
    public ResponseEntity<Resource> getManifestSchema11() {
        return ResponseEntity.ok()
                .contentType(JSON_SCHEMA_MEDIA_TYPE)
                .body(MANIFEST_SCHEMA_1_1);
    }
}
