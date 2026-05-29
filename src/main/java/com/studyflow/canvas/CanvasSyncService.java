package com.studyflow.canvas;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyflow.canvas.dto.CanvasConnectionResponse;
import com.studyflow.canvas.dto.CanvasSettingsRequest;
import com.studyflow.canvas.exception.CanvasApiException;
import com.studyflow.canvas.model.CanvasAssignment;
import com.studyflow.canvas.model.CanvasPlannerItem;
import com.studyflow.canvas.model.CanvasSource;
import com.studyflow.canvas.model.CanvasTaskBucket;
import com.studyflow.canvas.model.CanvasTaskStatus;
import com.studyflow.canvas.model.CanvasTodoItem;
import com.studyflow.canvas.model.Course;
import com.studyflow.canvas.model.StudyTask;
import com.studyflow.canvas.model.SyncLogEntry;
import com.studyflow.canvas.model.SyncResult;
import com.studyflow.service.AuthenticatedUserService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CanvasSyncService {

    private static final String CANVAS_BASE_URL_ENV = "STUDYFLOW_CANVAS_BASE_URL";
    private static final String CANVAS_TOKEN_ENV = "STUDYFLOW_CANVAS_TOKEN";

    private final CanvasLocalStore localStore;
    private final AuthenticatedUserService authenticatedUserService;
    private final ObjectMapper objectMapper;
    private final CanvasPriorityCalculator priorityCalculator;
    private final Clock clock;

    @Autowired
    public CanvasSyncService(
            CanvasLocalStore localStore,
            AuthenticatedUserService authenticatedUserService,
            ObjectMapper objectMapper,
            CanvasPriorityCalculator priorityCalculator
    ) {
        this(localStore, authenticatedUserService, objectMapper, priorityCalculator, Clock.systemDefaultZone());
    }

    CanvasSyncService(
            CanvasLocalStore localStore,
            AuthenticatedUserService authenticatedUserService,
            ObjectMapper objectMapper,
            CanvasPriorityCalculator priorityCalculator,
            Clock clock
    ) {
        this.localStore = localStore;
        this.authenticatedUserService = authenticatedUserService;
        this.objectMapper = objectMapper;
        this.priorityCalculator = priorityCalculator;
        this.clock = clock;
    }

    public CanvasConnectionResponse testConnection(CanvasSettingsRequest request) {
        CanvasApiClient connector = buildCanvasConnector(request);
        List<Course> courses = connector.getCourses();
        String message = courses.isEmpty()
                ? "Connected to Canvas, but no active courses were returned."
                : "Connected to Canvas. Found " + courses.size() + " active course(s).";
        return new CanvasConnectionResponse(true, message, courses.size());
    }

    public SyncResult sync(CanvasSettingsRequest request, String email) {
        Long userId = authenticatedUserService.getRequiredUserId(email);
        Instant startedAt = Instant.now(clock);
        try {
            SyncResult result = syncWithClient(userId, buildCanvasConnector(request));
            localStore.insertSyncLog(userId, startedAt, "SUCCESS", "Canvas sync completed.", result);
            return result;
        } catch (CanvasApiException ex) {
            localStore.insertSyncLog(userId, startedAt, "FAILED", ex.getMessage(), null);
            throw ex;
        }
    }

    public SyncResult syncMock(String email) {
        Long userId = authenticatedUserService.getRequiredUserId(email);
        Instant startedAt = Instant.now(clock);
        SyncResult result = syncWithClient(userId, new MockCanvasConnector(clock));
        localStore.insertSyncLog(userId, startedAt, "SUCCESS", "Mock Canvas sync completed.", result);
        return result;
    }

    public List<StudyTask> findTasks(CanvasTaskBucket bucket, String email) {
        Long userId = authenticatedUserService.getRequiredUserId(email);
        return localStore.findTasks(userId, bucket, clock);
    }

    public List<SyncLogEntry> findSyncLogs(String email) {
        Long userId = authenticatedUserService.getRequiredUserId(email);
        return localStore.findSyncLogs(userId, 10);
    }

    SyncResult syncWithClient(Long userId, CanvasApiClient connector) {
        Instant syncedAt = Instant.now(clock);
        List<String> warnings = new ArrayList<>();

        List<Course> courses = connector.getCourses();
        if (courses.isEmpty()) {
            warnings.add("Canvas returned no active courses.");
        }

        Map<Long, Course> coursesById = courses.stream()
                .filter(course -> course.id() != null)
                .collect(Collectors.toMap(Course::id, Function.identity(), (left, right) -> left));

        List<CanvasAssignment> assignments = new ArrayList<>();
        for (Course course : courses) {
            try {
                assignments.addAll(connector.getAssignments(course.id()).stream()
                        .filter(CanvasAssignment::published)
                        .toList());
            } catch (CanvasApiException ex) {
                warnings.add("Skipped assignments for course " + course.id() + ": " + ex.getMessage());
            }
        }

        List<CanvasTodoItem> todoItems = fetchTodoItems(connector, warnings);
        List<CanvasPlannerItem> plannerItems = fetchPlannerItems(connector, warnings);

        Map<Long, CanvasTodoItem> todoByAssignmentId = todoItems.stream()
                .filter(item -> item.assignmentId() != null)
                .collect(Collectors.toMap(CanvasTodoItem::assignmentId, Function.identity(), (left, right) -> left));
        Map<Long, CanvasPlannerItem> plannerByAssignmentId = plannerItems.stream()
                .filter(item -> item.assignmentId() != null)
                .collect(Collectors.toMap(CanvasPlannerItem::assignmentId, Function.identity(), (left, right) -> left));

        List<StudyTask> tasks = assignments.stream()
                .map(assignment -> toStudyTask(
                        assignment,
                        coursesById.get(assignment.courseId()),
                        todoByAssignmentId.get(assignment.id()),
                        plannerByAssignmentId.get(assignment.id()),
                        syncedAt
                ))
                .toList();

        int noDueDateCount = (int) tasks.stream()
                .filter(task -> task.status() == CanvasTaskStatus.NO_DUE_DATE)
                .count();
        if (noDueDateCount > 0) {
            warnings.add(noDueDateCount + " Canvas assignment(s) had no due date and were placed in No Due Date.");
        }

        int missingFromTodoCount = (int) tasks.stream()
                .filter(StudyTask::missingFromCanvasTodo)
                .count();

        localStore.saveSyncData(userId, courses, assignments, tasks);

        return new SyncResult(
                courses.size(),
                assignments.size(),
                todoItems.size(),
                plannerItems.size(),
                tasks.size(),
                missingFromTodoCount,
                syncedAt,
                List.copyOf(warnings)
        );
    }

    private List<CanvasTodoItem> fetchTodoItems(CanvasApiClient connector, List<String> warnings) {
        try {
            return connector.getTodoItems();
        } catch (CanvasApiException ex) {
            if (isOptionalEndpointFailure(ex)) {
                warnings.add("Canvas Todo API was unavailable: " + ex.getMessage());
                return List.of();
            }
            throw ex;
        }
    }

    private List<CanvasPlannerItem> fetchPlannerItems(CanvasApiClient connector, List<String> warnings) {
        try {
            return connector.getPlannerItems();
        } catch (CanvasApiException ex) {
            if (isOptionalEndpointFailure(ex)) {
                warnings.add("Canvas Planner API was unavailable: " + ex.getMessage());
                return List.of();
            }
            throw ex;
        }
    }

    private boolean isOptionalEndpointFailure(CanvasApiException ex) {
        return ex.getStatus() == HttpStatus.NOT_FOUND || ex.getStatus() == HttpStatus.BAD_REQUEST;
    }

    private StudyTask toStudyTask(
            CanvasAssignment assignment,
            Course course,
            CanvasTodoItem todoItem,
            CanvasPlannerItem plannerItem,
            Instant syncedAt
    ) {
        boolean completed = assignment.submitted()
                || (todoItem != null && todoItem.completed())
                || (plannerItem != null && plannerItem.completed());
        LocalDate dueDate = toLocalDate(assignment.dueAt());
        CanvasTaskStatus status = priorityCalculator.determineStatus(dueDate, completed, assignment.missing());
        int priorityScore = priorityCalculator.score(dueDate, status);
        Set<String> sources = new LinkedHashSet<>();
        sources.add(CanvasSource.ASSIGNMENT_API.label());
        if (todoItem != null) {
            sources.add(CanvasSource.TODO_API.label());
        }
        if (plannerItem != null) {
            sources.add(CanvasSource.PLANNER_API.label());
        }

        return new StudyTask(
                null,
                assignment.id(),
                assignment.courseId(),
                course == null ? "Canvas Course " + assignment.courseId() : course.name(),
                assignment.name(),
                assignment.description(),
                firstText(assignment.htmlUrl(), todoItem == null ? "" : todoItem.htmlUrl(), plannerItem == null ? "" : plannerItem.htmlUrl()),
                assignment.dueAt(),
                dueDate,
                status,
                priorityScore,
                priorityCalculator.label(priorityScore),
                List.copyOf(sources),
                todoItem == null,
                syncedAt
        );
    }

    private LocalDate toLocalDate(OffsetDateTime dueAt) {
        return dueAt == null ? null : dueAt.atZoneSameInstant(clock.getZone()).toLocalDate();
    }

    private CanvasApiClient buildCanvasConnector(CanvasSettingsRequest request) {
        String baseUrl = firstText(
                request == null ? "" : request.baseUrl(),
                System.getenv(CANVAS_BASE_URL_ENV)
        );
        String accessToken = firstText(
                request == null ? "" : request.accessToken(),
                System.getenv(CANVAS_TOKEN_ENV)
        );
        return new CanvasConnector(baseUrl, accessToken, objectMapper);
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }
}
