package com.studyflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CourseRequest(
        @NotBlank(message = "Course name is required")
        @Size(max = 120, message = "Course name must be at most 120 characters")
        String name,

        @Size(max = 1000, message = "Course description must be at most 1000 characters")
        String description
) {
}
