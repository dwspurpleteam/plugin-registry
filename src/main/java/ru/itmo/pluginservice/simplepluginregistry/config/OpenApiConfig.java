package ru.itmo.pluginservice.simplepluginregistry.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI pluginRegistryOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Simple Plugin Registry API")
                        .version("v1")
                        .description("REST API for publishing and reading plugin metadata and immutable versions."));
    }
}
