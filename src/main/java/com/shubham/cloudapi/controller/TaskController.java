package com.shubham.cloudapi.controller;

import com.shubham.cloudapi.dto.ErrorResponse;
import com.shubham.cloudapi.dto.TaskRequest;
import com.shubham.cloudapi.dto.TaskResponse;
import com.shubham.cloudapi.entity.TaskStatus;
import com.shubham.cloudapi.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Tasks", description = "Create, read, update, delete, and filter tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @Operation(summary = "Create a task", description = "Stores a new task and returns it with generated id and timestamps.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Task created",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed or the JSON body is invalid",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskRequest request) {
        TaskResponse created = taskService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @Operation(summary = "List tasks", description = "Returns every task, newest first.")
    @ApiResponse(responseCode = "200", description = "Tasks returned",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = TaskResponse.class))))
    public List<TaskResponse> findAll() {
        return taskService.findAll();
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Filter tasks by status", description = "Returns tasks whose status matches the path value.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matching tasks returned",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TaskResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Status is not PENDING, IN_PROGRESS, or COMPLETED",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public List<TaskResponse> findByStatus(
            @Parameter(description = "Task status", example = "PENDING")
            @PathVariable TaskStatus status) {
        return taskService.findByStatus(status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a task by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task found",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Id is not a number",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Task does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public TaskResponse findById(@PathVariable Long id) {
        return taskService.findById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a task", description = "Replaces title, description, status, and priority. Timestamps are managed by the server.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task updated",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed or the JSON body is invalid",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Task does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public TaskResponse update(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        return taskService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a task")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Task deleted"),
            @ApiResponse(responseCode = "404", description = "Task does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
