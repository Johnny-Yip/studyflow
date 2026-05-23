package com.studyflow.controller;

import com.studyflow.dto.TaskRequest;
import com.studyflow.dto.TaskResponse;
import com.studyflow.service.TaskService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/api/courses/{courseId}/tasks")
    public List<TaskResponse> getTasksByCourse(@PathVariable Long courseId) {
        return taskService.getTasksByCourse(courseId).stream()
                .map(TaskResponse::from)
                .toList();
    }

    @PostMapping("/api/courses/{courseId}/tasks")
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable Long courseId,
            @Valid @RequestBody TaskRequest request
    ) {
        TaskResponse response = TaskResponse.from(taskService.createTask(courseId, request));
        return ResponseEntity
                .created(URI.create("/api/tasks/" + response.id()))
                .body(response);
    }

    @GetMapping("/api/tasks/{id}")
    public TaskResponse getTask(@PathVariable Long id) {
        return TaskResponse.from(taskService.getTask(id));
    }

    @PutMapping("/api/tasks/{id}")
    public TaskResponse updateTask(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        return TaskResponse.from(taskService.updateTask(id, request));
    }

    @DeleteMapping("/api/tasks/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
