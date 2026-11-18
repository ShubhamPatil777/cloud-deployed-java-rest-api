package com.shubham.cloudapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shubham.cloudapi.dto.TaskRequest;
import com.shubham.cloudapi.entity.TaskPriority;
import com.shubham.cloudapi.entity.TaskStatus;
import com.shubham.cloudapi.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskRepository taskRepository;

    @BeforeEach
    void cleanDatabase() {
        taskRepository.deleteAll();
    }

    @Test
    void createTaskReturns201AndCanBeRetrieved() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new TaskRequest(
                                "Write deployment notes",
                                "Document the Ubuntu VM rollout",
                                TaskStatus.PENDING,
                                TaskPriority.HIGH))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/tasks/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Write deployment notes"))
                .andExpect(jsonPath("$.description").value("Document the Ubuntu VM rollout"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andReturn();

        Number id = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/tasks/{id}", id.longValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.longValue()))
                .andExpect(jsonPath("$.title").value("Write deployment notes"));
    }

    @Test
    void createAllowsMissingDescription() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Untitled details","status":"IN_PROGRESS","priority":"LOW"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value(nullValue()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void listTasksReturnsCreatedTasks() throws Exception {
        create("First", TaskStatus.PENDING, TaskPriority.LOW);
        create("Second", TaskStatus.COMPLETED, TaskPriority.MEDIUM);

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void updateTaskReplacesFields() throws Exception {
        long id = create("Draft", TaskStatus.PENDING, TaskPriority.LOW);

        mockMvc.perform(put("/api/tasks/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new TaskRequest(
                                "Published",
                                "Ready for review",
                                TaskStatus.COMPLETED,
                                TaskPriority.HIGH))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Published"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    @Test
    void deleteTaskRemovesIt() throws Exception {
        long id = create("Temporary", TaskStatus.PENDING, TaskPriority.MEDIUM);

        mockMvc.perform(delete("/api/tasks/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found with id " + id))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void filterByStatusReturnsOnlyMatchingTasks() throws Exception {
        create("Waiting", TaskStatus.PENDING, TaskPriority.LOW);
        create("Doing", TaskStatus.IN_PROGRESS, TaskPriority.HIGH);
        create("Done", TaskStatus.COMPLETED, TaskPriority.MEDIUM);

        mockMvc.perform(get("/api/tasks/status/{status}", "IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Doing"))
                .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"));
    }

    @Test
    void rejectInvalidTaskBody() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"  ","description":"x","priority":"LOW"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.path").value("/api/tasks"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void rejectUnknownStatusValue() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Bad status","status":"BLOCKED","priority":"LOW"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("BLOCKED")))
                .andExpect(jsonPath("$.message", containsString("PENDING")));
    }

    @Test
    void rejectUnknownStatusFilter() throws Exception {
        mockMvc.perform(get("/api/tasks/status/{status}", "NOT_A_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("NOT_A_STATUS")))
                .andExpect(jsonPath("$.message", containsString("COMPLETED")));
    }

    @Test
    void rejectNonNumericId() throws Exception {
        mockMvc.perform(get("/api/tasks/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("abc")));
    }

    @Test
    void missingTaskReturns404WithoutStackTrace() throws Exception {
        mockMvc.perform(get("/api/tasks/{id}", 404))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found with id 404"))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void updateMissingTaskReturns404() throws Exception {
        mockMvc.perform(put("/api/tasks/{id}", 50)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new TaskRequest("Title", null, TaskStatus.PENDING, TaskPriority.LOW))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteMissingTaskReturns404() throws Exception {
        mockMvc.perform(delete("/api/tasks/{id}", 51))
                .andExpect(status().isNotFound());
    }

    @Test
    void apiInfoReturnsNameDescriptionAndVersion() throws Exception {
        mockMvc.perform(get("/api/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cloud Task API"))
                .andExpect(jsonPath("$.description").value(containsString("Task management REST API")))
                .andExpect(jsonPath("$.version").value("1.0.0"));
    }

    @Test
    void actuatorHealthIsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    private long create(String title, TaskStatus status, TaskPriority priority) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new TaskRequest(title, "Details for " + title, status, priority))))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
