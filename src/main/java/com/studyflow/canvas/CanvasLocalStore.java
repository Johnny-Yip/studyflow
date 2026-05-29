package com.studyflow.canvas;

import com.studyflow.canvas.exception.CanvasApiException;
import com.studyflow.canvas.model.CanvasAssignment;
import com.studyflow.canvas.model.CanvasTaskBucket;
import com.studyflow.canvas.model.CanvasTaskStatus;
import com.studyflow.canvas.model.Course;
import com.studyflow.canvas.model.StudyTask;
import com.studyflow.canvas.model.SyncLogEntry;
import com.studyflow.canvas.model.SyncResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CanvasLocalStore {

    private final String databaseUrl;

    public CanvasLocalStore(@Value("${studyflow.canvas.database-url:}") String configuredDatabaseUrl) {
        this.databaseUrl = resolveDatabaseUrl(configuredDatabaseUrl);
        initialize();
    }

    public void initialize() {
        ensureParentDirectory();
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS courses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        local_user_id INTEGER NOT NULL,
                        canvas_course_id INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        course_code TEXT,
                        workflow_state TEXT,
                        synced_at TEXT NOT NULL,
                        UNIQUE(local_user_id, canvas_course_id)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS assignments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        local_user_id INTEGER NOT NULL,
                        canvas_assignment_id INTEGER NOT NULL,
                        canvas_course_id INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT,
                        html_url TEXT,
                        due_at TEXT,
                        published INTEGER NOT NULL,
                        submitted INTEGER NOT NULL,
                        missing INTEGER NOT NULL,
                        synced_at TEXT NOT NULL,
                        UNIQUE(local_user_id, canvas_assignment_id)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS tasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        local_user_id INTEGER NOT NULL,
                        canvas_assignment_id INTEGER NOT NULL,
                        canvas_course_id INTEGER NOT NULL,
                        course_name TEXT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT,
                        html_url TEXT,
                        due_at TEXT,
                        due_date TEXT,
                        status TEXT NOT NULL,
                        priority_score INTEGER NOT NULL,
                        priority_label TEXT NOT NULL,
                        sources TEXT NOT NULL,
                        missing_from_canvas_todo INTEGER NOT NULL,
                        synced_at TEXT NOT NULL,
                        UNIQUE(local_user_id, canvas_assignment_id)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS sync_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        local_user_id INTEGER NOT NULL,
                        started_at TEXT NOT NULL,
                        completed_at TEXT NOT NULL,
                        status TEXT NOT NULL,
                        message TEXT,
                        courses_fetched INTEGER NOT NULL,
                        assignments_fetched INTEGER NOT NULL,
                        todo_items_fetched INTEGER NOT NULL,
                        planner_items_fetched INTEGER NOT NULL,
                        tasks_upserted INTEGER NOT NULL,
                        missing_from_todo INTEGER NOT NULL
                    )
                    """);
        } catch (SQLException ex) {
            throw storageException();
        }
    }

    public void saveSyncData(Long userId, List<Course> courses, List<CanvasAssignment> assignments, List<StudyTask> tasks) {
        Instant syncedAt = Instant.now();

        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try {
                for (Course course : courses) {
                    upsertCourse(connection, userId, course, syncedAt);
                }
                for (CanvasAssignment assignment : assignments) {
                    upsertAssignment(connection, userId, assignment, syncedAt);
                }
                for (StudyTask task : tasks) {
                    upsertTask(connection, userId, task);
                }
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            throw storageException();
        }
    }

    public void insertSyncLog(Long userId, Instant startedAt, String status, String message, SyncResult result) {
        SyncResult safeResult = result == null
                ? new SyncResult(0, 0, 0, 0, 0, 0, Instant.now(), List.of())
                : result;

        String sql = """
                INSERT INTO sync_logs (
                    local_user_id, started_at, completed_at, status, message,
                    courses_fetched, assignments_fetched, todo_items_fetched, planner_items_fetched,
                    tasks_upserted, missing_from_todo
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, startedAt.toString());
            statement.setString(3, safeResult.syncedAt().toString());
            statement.setString(4, status);
            statement.setString(5, message);
            statement.setInt(6, safeResult.coursesFetched());
            statement.setInt(7, safeResult.assignmentsFetched());
            statement.setInt(8, safeResult.todoItemsFetched());
            statement.setInt(9, safeResult.plannerItemsFetched());
            statement.setInt(10, safeResult.tasksUpserted());
            statement.setInt(11, safeResult.missingFromTodoCount());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw storageException();
        }
    }

    public List<StudyTask> findTasks(Long userId, CanvasTaskBucket bucket, Clock clock) {
        String sql = """
                SELECT * FROM tasks
                WHERE local_user_id = ?
                ORDER BY
                    CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END,
                    priority_score DESC,
                    CASE WHEN due_date IS NULL THEN 1 ELSE 0 END,
                    due_date ASC,
                    title COLLATE NOCASE ASC
                """;

        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            ResultSet resultSet = statement.executeQuery();
            List<StudyTask> tasks = new ArrayList<>();
            while (resultSet.next()) {
                StudyTask task = mapTask(resultSet);
                if (matchesBucket(task, bucket, clock)) {
                    tasks.add(task);
                }
            }
            return tasks;
        } catch (SQLException ex) {
            throw storageException();
        }
    }

    public List<SyncLogEntry> findSyncLogs(Long userId, int limit) {
        String sql = """
                SELECT * FROM sync_logs
                WHERE local_user_id = ?
                ORDER BY completed_at DESC
                LIMIT ?
                """;
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setInt(2, limit);
            ResultSet resultSet = statement.executeQuery();
            List<SyncLogEntry> logs = new ArrayList<>();
            while (resultSet.next()) {
                logs.add(new SyncLogEntry(
                        resultSet.getLong("id"),
                        Instant.parse(resultSet.getString("started_at")),
                        Instant.parse(resultSet.getString("completed_at")),
                        resultSet.getString("status"),
                        resultSet.getString("message"),
                        resultSet.getInt("courses_fetched"),
                        resultSet.getInt("assignments_fetched"),
                        resultSet.getInt("todo_items_fetched"),
                        resultSet.getInt("planner_items_fetched"),
                        resultSet.getInt("tasks_upserted"),
                        resultSet.getInt("missing_from_todo")
                ));
            }
            return logs;
        } catch (SQLException ex) {
            throw storageException();
        }
    }

    private void upsertCourse(Connection connection, Long userId, Course course, Instant syncedAt) throws SQLException {
        String sql = """
                INSERT INTO courses (local_user_id, canvas_course_id, name, course_code, workflow_state, synced_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT(local_user_id, canvas_course_id) DO UPDATE SET
                    name = excluded.name,
                    course_code = excluded.course_code,
                    workflow_state = excluded.workflow_state,
                    synced_at = excluded.synced_at
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, course.id());
            statement.setString(3, course.name());
            statement.setString(4, course.courseCode());
            statement.setString(5, course.workflowState());
            statement.setString(6, syncedAt.toString());
            statement.executeUpdate();
        }
    }

    private void upsertAssignment(Connection connection, Long userId, CanvasAssignment assignment, Instant syncedAt) throws SQLException {
        String sql = """
                INSERT INTO assignments (
                    local_user_id, canvas_assignment_id, canvas_course_id, name, description, html_url,
                    due_at, published, submitted, missing, synced_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(local_user_id, canvas_assignment_id) DO UPDATE SET
                    canvas_course_id = excluded.canvas_course_id,
                    name = excluded.name,
                    description = excluded.description,
                    html_url = excluded.html_url,
                    due_at = excluded.due_at,
                    published = excluded.published,
                    submitted = excluded.submitted,
                    missing = excluded.missing,
                    synced_at = excluded.synced_at
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, assignment.id());
            statement.setLong(3, assignment.courseId());
            statement.setString(4, assignment.name());
            statement.setString(5, assignment.description());
            statement.setString(6, assignment.htmlUrl());
            statement.setString(7, format(assignment.dueAt()));
            statement.setInt(8, assignment.published() ? 1 : 0);
            statement.setInt(9, assignment.submitted() ? 1 : 0);
            statement.setInt(10, assignment.missing() ? 1 : 0);
            statement.setString(11, syncedAt.toString());
            statement.executeUpdate();
        }
    }

    private void upsertTask(Connection connection, Long userId, StudyTask task) throws SQLException {
        String sql = """
                INSERT INTO tasks (
                    local_user_id, canvas_assignment_id, canvas_course_id, course_name, title, description,
                    html_url, due_at, due_date, status, priority_score, priority_label, sources,
                    missing_from_canvas_todo, synced_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(local_user_id, canvas_assignment_id) DO UPDATE SET
                    canvas_course_id = excluded.canvas_course_id,
                    course_name = excluded.course_name,
                    title = excluded.title,
                    description = excluded.description,
                    html_url = excluded.html_url,
                    due_at = excluded.due_at,
                    due_date = excluded.due_date,
                    status = excluded.status,
                    priority_score = excluded.priority_score,
                    priority_label = excluded.priority_label,
                    sources = excluded.sources,
                    missing_from_canvas_todo = excluded.missing_from_canvas_todo,
                    synced_at = excluded.synced_at
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, task.canvasAssignmentId());
            statement.setLong(3, task.canvasCourseId());
            statement.setString(4, task.courseName());
            statement.setString(5, task.title());
            statement.setString(6, task.description());
            statement.setString(7, task.htmlUrl());
            statement.setString(8, format(task.dueAt()));
            statement.setString(9, format(task.dueDate()));
            statement.setString(10, task.status().name());
            statement.setInt(11, task.priorityScore());
            statement.setString(12, task.priorityLabel());
            statement.setString(13, String.join("|", task.sources()));
            statement.setInt(14, task.missingFromCanvasTodo() ? 1 : 0);
            statement.setString(15, task.syncedAt().toString());
            statement.executeUpdate();
        }
    }

    private StudyTask mapTask(ResultSet resultSet) throws SQLException {
        return new StudyTask(
                resultSet.getLong("id"),
                resultSet.getLong("canvas_assignment_id"),
                resultSet.getLong("canvas_course_id"),
                resultSet.getString("course_name"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                resultSet.getString("html_url"),
                parseOffsetDateTime(resultSet.getString("due_at")),
                parseLocalDate(resultSet.getString("due_date")),
                CanvasTaskStatus.valueOf(resultSet.getString("status")),
                resultSet.getInt("priority_score"),
                resultSet.getString("priority_label"),
                parseSources(resultSet.getString("sources")),
                resultSet.getInt("missing_from_canvas_todo") == 1,
                Instant.parse(resultSet.getString("synced_at"))
        );
    }

    private boolean matchesBucket(StudyTask task, CanvasTaskBucket bucket, Clock clock) {
        CanvasTaskBucket safeBucket = bucket == null ? CanvasTaskBucket.ALL : bucket;
        LocalDate today = LocalDate.now(clock);
        LocalDate dueDate = task.dueDate();

        return switch (safeBucket) {
            case ALL -> true;
            case TODAY -> dueDate != null && dueDate.isEqual(today);
            case THIS_WEEK -> dueDate != null && !dueDate.isBefore(today) && !dueDate.isAfter(today.plusDays(7));
            case OVERDUE -> task.status() == CanvasTaskStatus.OVERDUE || task.status() == CanvasTaskStatus.MISSING;
            case NO_DUE_DATE -> task.status() == CanvasTaskStatus.NO_DUE_DATE;
            case HIGH_PRIORITY -> task.priorityScore() >= 70 && task.status() != CanvasTaskStatus.COMPLETED;
        };
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(databaseUrl);
    }

    private String resolveDatabaseUrl(String configuredDatabaseUrl) {
        if (StringUtils.hasText(configuredDatabaseUrl)) {
            return configuredDatabaseUrl.trim();
        }
        Path defaultPath = Paths.get("data", "canvas-sync.db");
        return "jdbc:sqlite:" + defaultPath;
    }

    private void ensureParentDirectory() {
        if (!databaseUrl.startsWith("jdbc:sqlite:")) {
            return;
        }

        String rawPath = databaseUrl.substring("jdbc:sqlite:".length());
        if (!StringUtils.hasText(rawPath) || rawPath.equals(":memory:") || rawPath.startsWith("file:")) {
            return;
        }

        Path databasePath = Paths.get(rawPath);
        Path parent = databasePath.toAbsolutePath().getParent();
        if (parent == null) {
            return;
        }

        try {
            Files.createDirectories(parent);
        } catch (Exception ex) {
            throw storageException();
        }
    }

    private static String format(OffsetDateTime value) {
        return value == null ? null : value.toString();
    }

    private static String format(LocalDate value) {
        return value == null ? null : value.toString();
    }

    private static OffsetDateTime parseOffsetDateTime(String value) {
        return StringUtils.hasText(value) ? OffsetDateTime.parse(value) : null;
    }

    private static LocalDate parseLocalDate(String value) {
        return StringUtils.hasText(value) ? LocalDate.parse(value) : null;
    }

    private static List<String> parseSources(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split("\\|"))
                .filter(StringUtils::hasText)
                .toList();
    }

    private CanvasApiException storageException() {
        return new CanvasApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not access local Canvas SQLite storage.");
    }
}
