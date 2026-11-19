package ru.itmo.pluginservice.simplepluginregistry.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.itmo.pluginservice.simplepluginregistry.exception.ApiExceptionHandler;
import ru.itmo.pluginservice.simplepluginregistry.service.ManifestSchemaRegistry;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.http.MediaType.parseMediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ManifestSchemaControllerTest {
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ManifestSchemaController(new ManifestSchemaRegistry(new ObjectMapper())))
            .setControllerAdvice(new ApiExceptionHandler())
            .build();

    @Test
    void listsRegisteredVersions() throws Exception {
        mockMvc.perform(get("/v1/manifest-schemas"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.versions[0]").value("1.1"));
    }

    @Test
    void returnsManifestSchema11FromApplicationResources() throws Exception {
        mockMvc.perform(get("/v1/manifest-schemas/1.1").accept(parseMediaType("application/schema+json")))
                .andExpect(status().isOk()).andExpect(content().contentType("application/schema+json"))
                .andExpect(jsonPath("$.properties.manifest-version.const").value("1.1"));
    }

    @Test
    void returns404ForUnknownSchema() throws Exception {
        mockMvc.perform(get("/v1/manifest-schemas/9.9")).andExpect(status().isNotFound());
    }
}
