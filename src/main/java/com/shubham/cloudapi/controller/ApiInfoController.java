package com.shubham.cloudapi.controller;

import com.shubham.cloudapi.dto.ApiInfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/info")
@Tag(name = "API information", description = "Application name, description, and version")
public class ApiInfoController {

    private final String name;
    private final String description;
    private final String version;

    public ApiInfoController(
            @Value("${app.api.name}") String name,
            @Value("${app.api.description}") String description,
            @Value("${app.api.version}") String version) {
        this.name = name;
        this.description = description;
        this.version = version;
    }

    @GetMapping
    @Operation(summary = "Get API information")
    @ApiResponse(responseCode = "200", description = "Application metadata",
            content = @Content(schema = @Schema(implementation = ApiInfoResponse.class)))
    public ApiInfoResponse info() {
        return new ApiInfoResponse(name, description, version);
    }
}
