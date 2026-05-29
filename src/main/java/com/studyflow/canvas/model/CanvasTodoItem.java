package com.studyflow.canvas.model;

import java.time.OffsetDateTime;

public record CanvasTodoItem(
        Long assignmentId,
        Long courseId,
        String title,
        String htmlUrl,
        OffsetDateTime dueAt,
        boolean completed
) {
}
