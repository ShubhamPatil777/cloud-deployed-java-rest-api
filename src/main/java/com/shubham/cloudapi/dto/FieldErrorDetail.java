package com.shubham.cloudapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A single field validation failure")
public record FieldErrorDetail(
        @Schema(example = "title") String field,
        @Schema(example = "Title is required") String message
) {
}
