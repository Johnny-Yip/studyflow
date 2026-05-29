package com.studyflow.canvas;

import com.studyflow.canvas.model.CanvasAssignment;
import com.studyflow.canvas.model.CanvasPlannerItem;
import com.studyflow.canvas.model.CanvasTodoItem;
import com.studyflow.canvas.model.Course;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

public class MockCanvasConnector implements CanvasApiClient {

    private final Clock clock;

    public MockCanvasConnector(Clock clock) {
        this.clock = clock;
    }

    @Override
    public boolean testConnection() {
        return true;
    }

    @Override
    public List<Course> getCourses() {
        return List.of(
                new Course(101L, "Algorithms", "CS-241", "available"),
                new Course(202L, "Database Systems", "CS-332", "available")
        );
    }

    @Override
    public List<CanvasAssignment> getAssignments(Long courseId) {
        LocalDate today = LocalDate.now(clock);
        if (courseId.equals(101L)) {
            return List.of(
                    new CanvasAssignment(
                            1001L,
                            101L,
                            "Dynamic programming problem set",
                            "Practice recurrence relations and memoization.",
                            "https://canvas.example.edu/courses/101/assignments/1001",
                            atStartOfDay(today.plusDays(1)),
                            true,
                            false,
                            false
                    ),
                    new CanvasAssignment(
                            1002L,
                            101L,
                            "Graph traversal quiz",
                            "Short quiz covering DFS, BFS, and topological order.",
                            "https://canvas.example.edu/courses/101/assignments/1002",
                            atStartOfDay(today.minusDays(2)),
                            true,
                            false,
                            true
                    )
            );
        }

        if (courseId.equals(202L)) {
            return List.of(
                    new CanvasAssignment(
                            2001L,
                            202L,
                            "SQL normalization worksheet",
                            "Normalize a small registration schema.",
                            "https://canvas.example.edu/courses/202/assignments/2001",
                            atStartOfDay(today.plusDays(5)),
                            true,
                            false,
                            false
                    ),
                    new CanvasAssignment(
                            2002L,
                            202L,
                            "Index design reflection",
                            "Explain when an index helps and when it hurts write performance.",
                            "https://canvas.example.edu/courses/202/assignments/2002",
                            null,
                            true,
                            false,
                            false
                    )
            );
        }

        return List.of();
    }

    @Override
    public List<CanvasTodoItem> getTodoItems() {
        LocalDate today = LocalDate.now(clock);
        return List.of(
                new CanvasTodoItem(
                        1001L,
                        101L,
                        "Dynamic programming problem set",
                        "https://canvas.example.edu/courses/101/assignments/1001",
                        atStartOfDay(today.plusDays(1)),
                        false
                ),
                new CanvasTodoItem(
                        2001L,
                        202L,
                        "SQL normalization worksheet",
                        "https://canvas.example.edu/courses/202/assignments/2001",
                        atStartOfDay(today.plusDays(5)),
                        false
                )
        );
    }

    @Override
    public List<CanvasPlannerItem> getPlannerItems() {
        LocalDate today = LocalDate.now(clock);
        return List.of(
                new CanvasPlannerItem(
                        1001L,
                        101L,
                        "Dynamic programming problem set",
                        "https://canvas.example.edu/courses/101/assignments/1001",
                        atStartOfDay(today.plusDays(1)),
                        false
                ),
                new CanvasPlannerItem(
                        1002L,
                        101L,
                        "Graph traversal quiz",
                        "https://canvas.example.edu/courses/101/assignments/1002",
                        atStartOfDay(today.minusDays(2)),
                        false
                )
        );
    }

    private OffsetDateTime atStartOfDay(LocalDate date) {
        return date.atStartOfDay().atOffset(ZoneOffset.UTC);
    }
}
