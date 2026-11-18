package ru.itmo.pluginservice.simplepluginregistry.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.http.MediaType.parseMediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ManifestSchemaControllerTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ManifestSchemaController())
            .build();

    @Test
    void returnsManifestSchema11FromApplicationResources() throws Exception {
        mockMvc.perform(get("/v1/manifest-schemas/1.1")
                        .accept(parseMediaType("application/schema+json")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/schema+json"))
                .andExpect(jsonPath("$.properties.manifest-version.const").value("1.1"))
                .andExpect(jsonPath("$.$defs.mediaTypes.items.enum", hasItems(
                        "AUDIO",
                        "VIDEO",
                        "SCREEN_SHARE",
                        "SCREEN_SHARE_AUDIO"
                )))
                .andExpect(jsonPath("$.$defs.events.properties.available-event-groups.items.enum", hasItems(
                        "participant",
                        "media",
                        "interaction",
                        "call"
                )));
    }
}
