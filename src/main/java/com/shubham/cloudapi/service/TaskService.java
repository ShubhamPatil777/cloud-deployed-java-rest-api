package com.shubham.cloudapi.service;

import com.shubham.cloudapi.dto.TaskRequest;
import com.shubham.cloudapi.dto.TaskResponse;
import com.shubham.cloudapi.entity.TaskStatus;

import java.util.List;

public interface TaskService {

    TaskResponse create(TaskRequest request);

    List<TaskResponse> findAll();

    TaskResponse findById(Long id);

    TaskResponse update(Long id, TaskRequest request);

    void delete(Long id);

    List<TaskResponse> findByStatus(TaskStatus status);
}
