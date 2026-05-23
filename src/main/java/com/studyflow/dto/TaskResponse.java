package com.studyflow.dto;

import com.studyflow.entity.Priority;
import com.studyflow.entity.Task;
import com.studyflow.entity.TaskStatus;
import java.time.LocalDate;

public record TaskResponse(
        Long id,
        String title,
        String description,
        LocalDate dueDate,
        Priority priority,
        TaskStatus status,
        Long courseId,
        String courseName
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getDueDate(),
                task.getPriority(),
                task.getStatus(),
                task.getCourse().getId(),
                task.getCourse().getName()
        );
    }
}
