package com.shubham.cloudapi.dto;

import com.shubham.cloudapi.entity.TaskPriority;
import com.shubham.cloudapi.entity.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Task returned by the API")
public record TaskResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Write deployment notes") String title,
        @Schema(example = "Document the Ubuntu VM rollout steps") String description,
        @Schema(example = "PENDING") TaskStatus status,
        @Schema(example = "MEDIUM") TaskPriority priority,
        Instant createdAt,
        Instant updatedAt
) {
}
