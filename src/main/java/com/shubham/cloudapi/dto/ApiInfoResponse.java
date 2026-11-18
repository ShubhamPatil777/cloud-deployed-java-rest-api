package com.shubham.cloudapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Public application metadata")
public record ApiInfoResponse(
        @Schema(example = "Cloud Task API") String name,
        @Schema(example = "Task management REST API demonstrating a Spring Boot, Docker, and CI/CD lifecycle.")
        String description,
        @Schema(example = "1.0.0") String version
) {
}
