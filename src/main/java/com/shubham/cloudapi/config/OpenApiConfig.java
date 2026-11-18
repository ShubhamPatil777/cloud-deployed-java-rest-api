package com.shubham.cloudapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cloudTaskOpenApi(
            @Value("${app.api.name}") String name,
            @Value("${app.api.description}") String description,
            @Value("${app.api.version}") String version) {
        return new OpenAPI()
                .info(new Info()
                        .title(name)
                        .description(description + " Interactive documentation for every endpoint, including request and response schemas and error bodies.")
                        .version(version))
                .addServersItem(new Server().url("/").description("Current host"));
    }
}
