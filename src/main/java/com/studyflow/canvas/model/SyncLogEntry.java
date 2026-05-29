package com.studyflow.canvas.model;

import java.time.Instant;

public record SyncLogEntry(
        Long id,
        Instant startedAt,
        Instant completedAt,
        String status,
        String message,
        int coursesFetched,
        int assignmentsFetched,
        int todoItemsFetched,
        int plannerItemsFetched,
        int tasksUpserted,
        int missingFromTodoCount
) {
}
