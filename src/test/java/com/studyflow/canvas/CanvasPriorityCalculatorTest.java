package com.studyflow.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.studyflow.canvas.model.CanvasTaskStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class CanvasPriorityCalculatorTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-05-29T08:00:00Z"),
            ZoneOffset.UTC
    );

    private final CanvasPriorityCalculator calculator = new CanvasPriorityCalculator(FIXED_CLOCK);

    @Test
    void overdueAssignmentsReceiveHighPriority() {
        LocalDate yesterday = LocalDate.of(2026, 5, 28);

        CanvasTaskStatus status = calculator.determineStatus(yesterday, false, false);
        int score = calculator.score(yesterday, status);

        assertEquals(CanvasTaskStatus.OVERDUE, status);
        assertTrue(score >= 89);
        assertEquals("High", calculator.label(score));
    }

    @Test
    void completedAssignmentsReceiveLowPriority() {
        LocalDate today = LocalDate.of(2026, 5, 29);

        CanvasTaskStatus status = calculator.determineStatus(today, true, false);
        int score = calculator.score(today, status);

        assertEquals(CanvasTaskStatus.COMPLETED, status);
        assertEquals(8, score);
        assertEquals("Low", calculator.label(score));
    }

    @Test
    void nullDueDatesBecomeMediumPriorityNoDueDateTasks() {
        CanvasTaskStatus status = calculator.determineStatus(null, false, false);
        int score = calculator.score(null, status);

        assertEquals(CanvasTaskStatus.NO_DUE_DATE, status);
        assertEquals(50, score);
        assertEquals("Medium", calculator.label(score));
    }
}
