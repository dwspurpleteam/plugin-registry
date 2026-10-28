package ru.itmo.pluginservice.simplepluginregistry.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.itmo.pluginservice.simplepluginregistry.dto.PluginResponse;
import ru.itmo.pluginservice.simplepluginregistry.dto.PluginVersionResponse;
import ru.itmo.pluginservice.simplepluginregistry.exception.ApiExceptionHandler;
import ru.itmo.pluginservice.simplepluginregistry.service.PluginRegistryService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PluginRegistryControllerTest {

    @Mock
    private PluginRegistryService registryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PluginRegistryController(registryService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void createsPlugin() throws Exception {
        when(registryService.createPlugin(any()))
                .thenReturn(new PluginResponse("plugin-1", "Test plugin", "Description"));

        mockMvc.perform(post("/v1/plugins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": "plugin-1",
                                  "name": "Test plugin",
                                  "description": "Description"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("plugin-1"));
    }

    @Test
    void createsPluginVersionFromTypedRequest() throws Exception {
        when(registryService.createVersion(eq("plugin-1"), any()))
                .thenReturn(new PluginVersionResponse(
                        "plugin-1", "1.0.0", "Test plugin", "Description", "1.1.0", "2.1.0"
                ));

        mockMvc.perform(post("/v1/plugins/plugin-1/versions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "manifestVersion": "0.1",
                                  "pluginVersion": "1.0.0",
                                  "frontend": {
                                    "sdkVersion": "1.1.0",
                                    "urlWeb": "https://example.com/ui"
                                  },
                                  "backend": {
                                    "sdkVersion": "2.1.0",
                                    "healthUrl": "https://example.com/health",
                                    "events": {
                                      "eventUrl": "https://example.com/events",
                                      "availableEventTypes": [],
                                      "availableEventGroups": ["participant"]
                                    },
                                    "media": {
                                      "input": ["AUDIO"],
                                      "output": ["VIDEO"]
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pluginId").value("plugin-1"))
                .andExpect(jsonPath("$.version").value("1.0.0"));
    }

    @Test
    void rejectsInvalidPluginRequest() throws Exception {
        mockMvc.perform(post("/v1/plugins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":"", "name":"", "description":null}
                                """))
                .andExpect(status().isBadRequest());
    }
}
