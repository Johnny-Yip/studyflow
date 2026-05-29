package com.studyflow.canvas.model;

import java.time.OffsetDateTime;

public record CanvasPlannerItem(
        Long assignmentId,
        Long courseId,
        String title,
        String htmlUrl,
        OffsetDateTime dueAt,
        boolean completed
) {
}
