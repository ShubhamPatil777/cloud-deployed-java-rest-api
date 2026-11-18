package com.shubham.cloudapi.repository;

import com.shubham.cloudapi.entity.Task;
import com.shubham.cloudapi.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findAllByOrderByCreatedAtDesc();

    List<Task> findByStatusOrderByCreatedAtDesc(TaskStatus status);
}
