package com.studyflow.canvas.model;

import java.time.Instant;
import java.util.List;

public record SyncResult(
        int coursesFetched,
        int assignmentsFetched,
        int todoItemsFetched,
        int plannerItemsFetched,
        int tasksUpserted,
        int missingFromTodoCount,
        Instant syncedAt,
        List<String> warnings
) {
}
