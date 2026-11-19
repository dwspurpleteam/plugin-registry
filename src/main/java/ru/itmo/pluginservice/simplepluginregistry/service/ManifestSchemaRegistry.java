package ru.itmo.pluginservice.simplepluginregistry.service;

import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SchemaRegistryConfig;
import com.networknt.schema.SpecificationVersion;
import com.networknt.schema.dialect.Dialect;
import com.networknt.schema.dialect.Dialects;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ManifestSchemaRegistry {

    static final String RESOURCE_PATTERN = "classpath*:plugin-manifest/*.schema.json";
    private static final Pattern FILE_NAME = Pattern.compile("^(.+)\\.schema\\.json$");
    private static final String DIALECT_2020_12 = "https://json-schema.org/draft/2020-12/schema";

    private final Map<String, RegisteredSchema> schemas;

    @Autowired
    public ManifestSchemaRegistry(ObjectMapper objectMapper) {
        this(objectMapper, new PathMatchingResourcePatternResolver(), RESOURCE_PATTERN);
    }

    ManifestSchemaRegistry(ObjectMapper objectMapper, PathMatchingResourcePatternResolver resolver, String pattern) {
        this.schemas = Collections.unmodifiableMap(load(objectMapper, resolver, pattern));
        if (schemas.isEmpty()) throw new IllegalStateException("No manifest schemas found at " + pattern);
    }

    public RegisteredSchema get(String version) {
        return schemas.get(version);
    }

    public List<String> supportedVersions() {
        return List.copyOf(schemas.keySet());
    }

    private Map<String, RegisteredSchema> load(
            ObjectMapper objectMapper,
            PathMatchingResourcePatternResolver resolver,
            String pattern
    ) {
        try {
            Resource[] resources = resolver.getResources(pattern);
            Arrays.sort(resources, (left, right) -> left.getFilename().compareTo(right.getFilename()));
            Map<String, RegisteredSchema> result = new LinkedHashMap<>();
            SchemaRegistry compiler = schemaCompiler();
            for (Resource resource : resources) {
                String fileName = resource.getFilename();
                Matcher matcher = FILE_NAME.matcher(fileName == null ? "" : fileName);
                if (!matcher.matches()) throw new IllegalStateException("Invalid manifest schema filename: " + fileName);
                String version = matcher.group(1);
                byte[] source;
                try (InputStream input = resource.getInputStream()) {
                    source = input.readAllBytes();
                }
                JsonNode document = objectMapper.readTree(source);
                validateMetadata(version, fileName, document);
                Schema compiled = compiler.getSchema(
                        SchemaLocation.of("urn:plugin-registry:manifest-schema:" + version),
                        new ByteArrayInputStream(source),
                        InputFormat.JSON
                );
                RegisteredSchema duplicate = result.putIfAbsent(version, new RegisteredSchema(version, compiled, source));
                if (duplicate != null) throw new IllegalStateException("Duplicate manifest schema version: " + version);
            }
            return result;
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof IllegalStateException illegalStateException) throw illegalStateException;
            throw new IllegalStateException("Failed to initialize manifest schema registry", exception);
        }
    }

    private SchemaRegistry schemaCompiler() {
        SchemaRegistryConfig config = SchemaRegistryConfig.builder().formatAssertionsEnabled(true).build();
        Dialect dialect = Dialect.builder(DIALECT_2020_12, Dialects.getDraft202012())
                .format(ManifestUrlFormat.HTTPS_URI)
                .format(ManifestUrlFormat.PUBLIC_HTTPS_URI)
                .build();
        return SchemaRegistry.withDefaultDialect(dialect, builder -> builder.schemaRegistryConfig(config));
    }

    private void validateMetadata(String version, String fileName, JsonNode document) {
        if (!document.isObject()) throw new IllegalStateException("Manifest schema must be a JSON object: " + fileName);
        JsonNode dialect = document.get("$schema");
        if (dialect == null || !DIALECT_2020_12.equals(dialect.asString())) {
            throw new IllegalStateException("Unsupported or missing JSON Schema dialect in " + fileName
                    + "; expected " + DIALECT_2020_12);
        }
        JsonNode declaredVersion = document.at("/properties/manifest-version/const");
        if (!declaredVersion.isTextual() || !version.equals(declaredVersion.asString())) {
            throw new IllegalStateException("Schema " + fileName + " must allow manifest-version '" + version
                    + "' via properties.manifest-version.const");
        }
    }

    public record RegisteredSchema(String version, Schema compiled, byte[] source) {
        public RegisteredSchema {
            source = source.clone();
        }

        @Override
        public byte[] source() {
            return source.clone();
        }
    }
}
