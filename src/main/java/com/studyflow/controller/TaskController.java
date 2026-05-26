package com.studyflow.controller;

import com.studyflow.dto.TaskRequest;
import com.studyflow.dto.TaskResponse;
import com.studyflow.entity.Priority;
import com.studyflow.entity.TaskStatus;
import com.studyflow.service.TaskService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/api/tasks")
    public List<TaskResponse> searchTasks(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) String title,
            @RequestParam(required = false, defaultValue = "dueDate") String sort,
            Authentication authentication
    ) {
        return taskService.searchTasks(courseId, status, priority, title, sort, authentication.getName()).stream()
                .map(TaskResponse::from)
                .toList();
    }

    @GetMapping("/api/courses/{courseId}/tasks")
    public List<TaskResponse> getTasksByCourse(@PathVariable Long courseId, Authentication authentication) {
        return taskService.getTasksByCourse(courseId, authentication.getName()).stream()
                .map(TaskResponse::from)
                .toList();
    }

    @PostMapping("/api/courses/{courseId}/tasks")
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable Long courseId,
            @Valid @RequestBody TaskRequest request,
            Authentication authentication
    ) {
        TaskResponse response = TaskResponse.from(taskService.createTask(courseId, request, authentication.getName()));
        return ResponseEntity
                .created(URI.create("/api/tasks/" + response.id()))
                .body(response);
    }

    @GetMapping("/api/tasks/{id}")
    public TaskResponse getTask(@PathVariable Long id, Authentication authentication) {
        return TaskResponse.from(taskService.getTask(id, authentication.getName()));
    }

    @PutMapping("/api/tasks/{id}")
    public TaskResponse updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request,
            Authentication authentication
    ) {
        return TaskResponse.from(taskService.updateTask(id, request, authentication.getName()));
    }

    @DeleteMapping("/api/tasks/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id, Authentication authentication) {
        taskService.deleteTask(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
