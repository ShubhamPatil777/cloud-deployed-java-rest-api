package com.shubham.cloudapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Structured API error. Stack traces are never included.")
public record ErrorResponse(
        Instant timestamp,
        @Schema(example = "400") int status,
        @Schema(example = "Bad Request") String error,
        @Schema(example = "Request validation failed") String message,
        @Schema(example = "/api/tasks") String path,
        List<FieldErrorDetail> fieldErrors
) {
}
