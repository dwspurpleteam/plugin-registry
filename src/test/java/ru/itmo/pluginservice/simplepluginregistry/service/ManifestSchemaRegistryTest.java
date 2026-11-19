package ru.itmo.pluginservice.simplepluginregistry.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import tools.jackson.databind.ObjectMapper;

import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ManifestSchemaRegistryTest {
    @TempDir
    Path tempDir;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void discoversProductionSchema11Automatically() {
        assertThat(new ManifestSchemaRegistry(objectMapper).supportedVersions()).contains("1.1");
    }

    @Test
    void discoversSchema12WithoutJavaChanges() {
        ManifestSchemaRegistry registry = new ManifestSchemaRegistry(objectMapper,
                new PathMatchingResourcePatternResolver(), "classpath*:test-plugin-manifest/*.schema.json");
        assertThat(registry.supportedVersions()).containsExactly("1.2");
        assertThat(registry.get("1.2").source()).isNotEmpty();
    }

    @Test
    void discoversSchemasFromJarClasspath() throws Exception {
        Path jar = tempDir.resolve("schemas.jar");
        byte[] schema = getClass().getResourceAsStream("/test-plugin-manifest/1.2.schema.json").readAllBytes();
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
            output.putNextEntry(new JarEntry("plugin-manifest/"));
            output.closeEntry();
            output.putNextEntry(new JarEntry("plugin-manifest/1.2.schema.json"));
            output.write(schema);
            output.closeEntry();
        }
        try (URLClassLoader classLoader = new URLClassLoader(new java.net.URL[]{jar.toUri().toURL()}, null)) {
            ManifestSchemaRegistry registry = new ManifestSchemaRegistry(objectMapper,
                    new PathMatchingResourcePatternResolver(classLoader), ManifestSchemaRegistry.RESOURCE_PATTERN);
            assertThat(registry.supportedVersions()).containsExactly("1.2");
        }
    }
}
