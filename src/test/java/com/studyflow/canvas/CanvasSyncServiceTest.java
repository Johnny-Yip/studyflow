package com.studyflow.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyflow.canvas.model.CanvasTaskBucket;
import com.studyflow.canvas.model.CanvasTaskStatus;
import com.studyflow.canvas.model.StudyTask;
import com.studyflow.canvas.model.SyncResult;
import com.studyflow.service.AuthenticatedUserService;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CanvasSyncServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-05-29T08:00:00Z"),
            ZoneOffset.UTC
    );

    @TempDir
    private Path tempDir;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Test
    void mockSyncStoresCanvasTasksAndDetectsItemsMissingFromTodo() {
        when(authenticatedUserService.getRequiredUserId("student@example.com")).thenReturn(7L);
        CanvasSyncService service = buildService();

        SyncResult result = service.syncMock("student@example.com");
        List<StudyTask> tasks = service.findTasks(CanvasTaskBucket.ALL, "student@example.com");
        List<StudyTask> noDueDateTasks = service.findTasks(CanvasTaskBucket.NO_DUE_DATE, "student@example.com");
        List<StudyTask> highPriorityTasks = service.findTasks(CanvasTaskBucket.HIGH_PRIORITY, "student@example.com");

        assertEquals(2, result.coursesFetched());
        assertEquals(4, result.assignmentsFetched());
        assertEquals(4, result.tasksUpserted());
        assertEquals(2, result.missingFromTodoCount());
        assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("no due date")));
        assertEquals(4, tasks.size());
        assertEquals(1, noDueDateTasks.size());
        assertEquals(CanvasTaskStatus.NO_DUE_DATE, noDueDateTasks.get(0).status());
        assertFalse(highPriorityTasks.isEmpty());
        assertTrue(tasks.stream().anyMatch(StudyTask::missingFromCanvasTodo));
        assertTrue(tasks.stream().anyMatch(task -> task.sources().contains("Planner API")));
    }

    private CanvasSyncService buildService() {
        CanvasLocalStore localStore = new CanvasLocalStore("jdbc:sqlite:" + tempDir.resolve("canvas-test.db"));
        CanvasPriorityCalculator priorityCalculator = new CanvasPriorityCalculator(FIXED_CLOCK);
        return new CanvasSyncService(
                localStore,
                authenticatedUserService,
                new ObjectMapper(),
                priorityCalculator,
                FIXED_CLOCK
        );
    }
}
