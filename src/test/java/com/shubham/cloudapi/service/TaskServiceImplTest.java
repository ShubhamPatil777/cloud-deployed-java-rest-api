package com.shubham.cloudapi.service;

import com.shubham.cloudapi.dto.TaskRequest;
import com.shubham.cloudapi.dto.TaskResponse;
import com.shubham.cloudapi.entity.Task;
import com.shubham.cloudapi.entity.TaskPriority;
import com.shubham.cloudapi.entity.TaskStatus;
import com.shubham.cloudapi.exception.TaskNotFoundException;
import com.shubham.cloudapi.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskServiceImpl taskService;

    @Test
    void createPersistsTrimmedTaskAndReturnsGeneratedId() {
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(7L);
            task.setCreatedAt(Instant.parse("2026-11-19T00:00:00Z"));
            task.setUpdatedAt(Instant.parse("2026-11-19T00:00:00Z"));
            return task;
        });

        TaskResponse response = taskService.create(new TaskRequest(
                "  Ship the API  ",
                "  ",
                TaskStatus.PENDING,
                TaskPriority.HIGH
        ));

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("Ship the API");
        assertThat(captor.getValue().getDescription()).isNull();
        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.title()).isEqualTo("Ship the API");
        assertThat(response.status()).isEqualTo(TaskStatus.PENDING);
        assertThat(response.priority()).isEqualTo(TaskPriority.HIGH);
        assertThat(response.createdAt()).isEqualTo(Instant.parse("2026-11-19T00:00:00Z"));
    }

    @Test
    void findByIdReturnsTaskWhenPresent() {
        when(taskRepository.findById(3L)).thenReturn(Optional.of(task(3L, "Read logs", TaskStatus.IN_PROGRESS)));

        TaskResponse response = taskService.findById(3L);

        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.title()).isEqualTo("Read logs");
        assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void findByIdThrowsWhenTaskIsMissing() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.findById(99L))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessage("Task not found with id 99");
    }

    @Test
    void updateReplacesMutableFields() {
        Task existing = task(4L, "Old title", TaskStatus.PENDING);
        when(taskRepository.findById(4L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(existing)).thenReturn(existing);

        TaskResponse response = taskService.update(4L, new TaskRequest(
                "New title",
                "Updated description",
                TaskStatus.COMPLETED,
                TaskPriority.LOW
        ));

        assertThat(existing.getTitle()).isEqualTo("New title");
        assertThat(existing.getDescription()).isEqualTo("Updated description");
        assertThat(existing.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(existing.getPriority()).isEqualTo(TaskPriority.LOW);
        assertThat(response.status()).isEqualTo(TaskStatus.COMPLETED);
    }

    @Test
    void updateThrowsWhenTaskIsMissing() {
        when(taskRepository.findById(8L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.update(8L, new TaskRequest(
                "Title", null, TaskStatus.PENDING, TaskPriority.MEDIUM)))
                .isInstanceOf(TaskNotFoundException.class);
        verify(taskRepository, never()).save(any());
    }

    @Test
    void deleteRemovesExistingTask() {
        Task existing = task(5L, "Remove me", TaskStatus.COMPLETED);
        when(taskRepository.findById(5L)).thenReturn(Optional.of(existing));

        taskService.delete(5L);

        verify(taskRepository).delete(existing);
    }

    @Test
    void deleteThrowsWhenTaskIsMissing() {
        when(taskRepository.findById(6L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.delete(6L))
                .isInstanceOf(TaskNotFoundException.class);
        verify(taskRepository, never()).delete(any());
    }

    @Test
    void findAllMapsRepositoryResults() {
        when(taskRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(
                task(2L, "Second", TaskStatus.PENDING),
                task(1L, "First", TaskStatus.COMPLETED)
        ));

        List<TaskResponse> tasks = taskService.findAll();

        assertThat(tasks).extracting(TaskResponse::title).containsExactly("Second", "First");
    }

    @Test
    void findByStatusDelegatesToRepository() {
        when(taskRepository.findByStatusOrderByCreatedAtDesc(TaskStatus.IN_PROGRESS))
                .thenReturn(List.of(task(9L, "Active", TaskStatus.IN_PROGRESS)));

        List<TaskResponse> tasks = taskService.findByStatus(TaskStatus.IN_PROGRESS);

        assertThat(tasks).hasSize(1);
        assertThat(tasks.getFirst().status()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    private Task task(Long id, String title, TaskStatus status) {
        Task task = new Task();
        task.setId(id);
        task.setTitle(title);
        task.setDescription("Description");
        task.setStatus(status);
        task.setPriority(TaskPriority.MEDIUM);
        task.setCreatedAt(Instant.parse("2026-11-19T00:00:00Z"));
        task.setUpdatedAt(Instant.parse("2026-11-19T00:00:00Z"));
        return task;
    }
}
