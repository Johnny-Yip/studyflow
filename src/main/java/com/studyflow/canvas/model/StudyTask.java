package com.studyflow.canvas.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record StudyTask(
        Long id,
        Long canvasAssignmentId,
        Long canvasCourseId,
        String courseName,
        String title,
        String description,
        String htmlUrl,
        OffsetDateTime dueAt,
        LocalDate dueDate,
        CanvasTaskStatus status,
        int priorityScore,
        String priorityLabel,
        List<String> sources,
        boolean missingFromCanvasTodo,
        Instant syncedAt
) {
}
