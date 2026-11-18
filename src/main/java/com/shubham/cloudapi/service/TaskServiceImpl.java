package com.shubham.cloudapi.service;

import com.shubham.cloudapi.dto.TaskRequest;
import com.shubham.cloudapi.dto.TaskResponse;
import com.shubham.cloudapi.entity.Task;
import com.shubham.cloudapi.entity.TaskStatus;
import com.shubham.cloudapi.exception.TaskNotFoundException;
import com.shubham.cloudapi.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TaskServiceImpl implements TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskServiceImpl.class);

    private final TaskRepository taskRepository;

    public TaskServiceImpl(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public TaskResponse create(TaskRequest request) {
        Task task = new Task();
        apply(task, request);
        Task saved = taskRepository.save(task);
        log.info("Created task id={}", saved.getId());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> findAll() {
        return taskRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse findById(Long id) {
        return toResponse(getRequired(id));
    }

    @Override
    public TaskResponse update(Long id, TaskRequest request) {
        Task task = getRequired(id);
        apply(task, request);
        Task saved = taskRepository.save(task);
        log.info("Updated task id={}", saved.getId());
        return toResponse(saved);
    }

    @Override
    public void delete(Long id) {
        Task task = getRequired(id);
        taskRepository.delete(task);
        log.info("Deleted task id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> findByStatus(TaskStatus status) {
        return taskRepository.findByStatusOrderByCreatedAtDesc(status).stream()
                .map(this::toResponse)
                .toList();
    }

    private Task getRequired(Long id) {
        return taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }

    private void apply(Task task, TaskRequest request) {
        task.setTitle(request.title().trim());
        task.setDescription(normalizeDescription(request.description()));
        task.setStatus(request.status());
        task.setPriority(request.priority());
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String trimmed = description.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
