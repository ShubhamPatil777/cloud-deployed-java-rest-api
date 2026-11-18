package com.shubham.cloudapi.dto;

import com.shubham.cloudapi.entity.TaskPriority;
import com.shubham.cloudapi.entity.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload used to create or replace a task")
public record TaskRequest(
        @Schema(description = "Short task title", example = "Write deployment notes", maxLength = 120)
        @NotBlank(message = "Title is required")
        @Size(max = 120, message = "Title must be at most 120 characters")
        String title,

        @Schema(description = "Optional longer description", example = "Document the Ubuntu VM rollout steps", maxLength = 2000)
        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        @Schema(description = "Current workflow status", example = "PENDING")
        @NotNull(message = "Status is required")
        TaskStatus status,

        @Schema(description = "Relative importance", example = "MEDIUM")
        @NotNull(message = "Priority is required")
        TaskPriority priority
) {
}
