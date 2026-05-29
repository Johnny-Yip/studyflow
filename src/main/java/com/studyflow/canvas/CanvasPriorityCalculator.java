package com.studyflow.canvas;

import com.studyflow.canvas.model.CanvasTaskStatus;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

@Component
public class CanvasPriorityCalculator {

    private final Clock clock;

    public CanvasPriorityCalculator() {
        this(Clock.systemDefaultZone());
    }

    CanvasPriorityCalculator(Clock clock) {
        this.clock = clock;
    }

    public CanvasTaskStatus determineStatus(LocalDate dueDate, boolean completed, boolean missing) {
        if (completed) {
            return CanvasTaskStatus.COMPLETED;
        }
        if (missing) {
            return CanvasTaskStatus.MISSING;
        }
        if (dueDate == null) {
            return CanvasTaskStatus.NO_DUE_DATE;
        }
        if (dueDate.isBefore(LocalDate.now(clock))) {
            return CanvasTaskStatus.OVERDUE;
        }
        return CanvasTaskStatus.UPCOMING;
    }

    public int score(LocalDate dueDate, CanvasTaskStatus status) {
        LocalDate today = LocalDate.now(clock);
        int score = switch (status) {
            case COMPLETED -> 8;
            case MISSING -> 96;
            case OVERDUE -> overdueScore(dueDate, today);
            case NO_DUE_DATE -> 50;
            case UPCOMING -> upcomingScore(dueDate, today);
        };
        return Math.max(0, Math.min(100, score));
    }

    public String label(int score) {
        if (score >= 70) {
            return "High";
        }
        if (score >= 40) {
            return "Medium";
        }
        return "Low";
    }

    private int overdueScore(LocalDate dueDate, LocalDate today) {
        if (dueDate == null) {
            return 90;
        }
        long overdueDays = Math.max(1, ChronoUnit.DAYS.between(dueDate, today));
        return 88 + (int) Math.min(12, overdueDays);
    }

    private int upcomingScore(LocalDate dueDate, LocalDate today) {
        if (dueDate == null) {
            return 50;
        }

        long daysUntilDue = ChronoUnit.DAYS.between(today, dueDate);
        if (daysUntilDue <= 0) {
            return 86;
        }
        if (daysUntilDue == 1) {
            return 80;
        }
        if (daysUntilDue <= 3) {
            return 72;
        }
        if (daysUntilDue <= 7) {
            return 60;
        }
        if (daysUntilDue <= 14) {
            return 45;
        }
        return 28;
    }
}
