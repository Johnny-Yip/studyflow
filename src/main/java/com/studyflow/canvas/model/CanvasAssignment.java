package com.studyflow.canvas.model;

import java.time.OffsetDateTime;

public record CanvasAssignment(
        Long id,
        Long courseId,
        String name,
        String description,
        String htmlUrl,
        OffsetDateTime dueAt,
        boolean published,
        boolean submitted,
        boolean missing
) {
}
