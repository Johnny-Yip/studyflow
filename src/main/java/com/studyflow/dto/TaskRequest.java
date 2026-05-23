package com.studyflow.dto;

import com.studyflow.entity.Priority;
import com.studyflow.entity.TaskStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TaskRequest(
        @NotBlank(message = "Task title is required")
        @Size(max = 160, message = "Task title must be at most 160 characters")
        String title,

        @Size(max = 2000, message = "Task description must be at most 2000 characters")
        String description,

        @NotNull(message = "Due date is required")
        @FutureOrPresent(message = "Due date must be today or in the future")
        LocalDate dueDate,

        @NotNull(message = "Priority is required")
        Priority priority,

        @NotNull(message = "Status is required")
        TaskStatus status
) {
}
