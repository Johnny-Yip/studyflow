package com.studyflow.canvas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyflow.canvas.exception.CanvasApiException;
import com.studyflow.canvas.model.CanvasAssignment;
import com.studyflow.canvas.model.CanvasPlannerItem;
import com.studyflow.canvas.model.CanvasTodoItem;
import com.studyflow.canvas.model.Course;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

public class CanvasConnector implements CanvasApiClient {

    private static final int MAX_PAGES = 100;
    private static final Pattern NEXT_LINK_PATTERN = Pattern.compile("<([^>]+)>;\\s*rel=\"next\"");

    private final URI baseUri;
    private final String accessToken;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public CanvasConnector(String baseUrl, String accessToken, ObjectMapper objectMapper) {
        this(baseUrl, accessToken, objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build());
    }

    CanvasConnector(String baseUrl, String accessToken, ObjectMapper objectMapper, HttpClient httpClient) {
        this.baseUri = normalizeBaseUri(baseUrl);
        this.accessToken = requireToken(accessToken);
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public boolean testConnection() {
        fetchPaginatedArray("/api/v1/courses?per_page=1");
        return true;
    }

    @Override
    public List<Course> getCourses() {
        return fetchPaginatedArray("/api/v1/courses?enrollment_state=active&per_page=100").stream()
                .map(this::parseCourse)
                .filter(Course::isActive)
                .toList();
    }

    @Override
    public List<CanvasAssignment> getAssignments(Long courseId) {
        String encodedCourseId = encodePathSegment(courseId);
        return fetchPaginatedArray("/api/v1/courses/" + encodedCourseId + "/assignments?per_page=100").stream()
                .map(node -> parseAssignment(node, courseId))
                .filter(assignment -> assignment.id() != null)
                .toList();
    }

    @Override
    public List<CanvasTodoItem> getTodoItems() {
        return fetchPaginatedArray("/api/v1/users/self/todo?per_page=100").stream()
                .map(this::parseTodoItem)
                .filter(item -> item.assignmentId() != null)
                .toList();
    }

    @Override
    public List<CanvasPlannerItem> getPlannerItems() {
        try {
            return fetchPlannerItems("/api/v1/planner/items?per_page=100");
        } catch (CanvasApiException ex) {
            if (ex.getStatus() != HttpStatus.NOT_FOUND && ex.getStatus() != HttpStatus.BAD_REQUEST) {
                throw ex;
            }
            return fetchPlannerItems("/api/v1/users/self/planner/items?per_page=100");
        }
    }

    private List<CanvasPlannerItem> fetchPlannerItems(String path) {
        return fetchPaginatedArray(path).stream()
                .map(this::parsePlannerItem)
                .filter(item -> item.assignmentId() != null)
                .toList();
    }

    private List<JsonNode> fetchPaginatedArray(String path) {
        List<JsonNode> results = new ArrayList<>();
        URI nextUri = resolve(path);
        int pagesFetched = 0;

        while (nextUri != null) {
            if (++pagesFetched > MAX_PAGES) {
                throw new CanvasApiException(HttpStatus.BAD_GATEWAY, "Canvas pagination exceeded the safety limit.");
            }

            HttpResponse<String> response = sendGet(nextUri);
            JsonNode root = readJson(response.body());
            if (!root.isArray()) {
                throw new CanvasApiException(HttpStatus.BAD_GATEWAY, "Canvas returned an unexpected response shape.");
            }

            root.forEach(results::add);
            nextUri = nextLink(response).map(URI::create).orElse(null);
        }

        return results;
    }

    private HttpResponse<String> sendGet(URI uri) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw mapHttpError(response.statusCode());
            }
            return response;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CanvasApiException(HttpStatus.SERVICE_UNAVAILABLE, "Canvas request was interrupted.");
        } catch (IOException ex) {
            throw new CanvasApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Could not reach Canvas. Check the Canvas URL and network connection."
            );
        }
    }

    private CanvasApiException mapHttpError(int statusCode) {
        if (statusCode == 401 || statusCode == 403) {
            return new CanvasApiException(
                    HttpStatus.valueOf(statusCode),
                    "Canvas rejected the access token or does not allow this API call."
            );
        }
        if (statusCode == 404) {
            return new CanvasApiException(HttpStatus.NOT_FOUND, "Canvas endpoint was not found on this instance.");
        }
        if (statusCode == 429) {
            return new CanvasApiException(HttpStatus.TOO_MANY_REQUESTS, "Canvas API rate limit reached. Try again later.");
        }
        if (statusCode >= 500) {
            return new CanvasApiException(HttpStatus.BAD_GATEWAY, "Canvas is temporarily unavailable.");
        }
        return new CanvasApiException(HttpStatus.BAD_REQUEST, "Canvas rejected the request.");
    }

    private JsonNode readJson(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (IOException ex) {
            throw new CanvasApiException(HttpStatus.BAD_GATEWAY, "Canvas returned malformed JSON.");
        }
    }

    private Optional<String> nextLink(HttpResponse<?> response) {
        return response.headers().firstValue("Link")
                .flatMap(linkHeader -> {
                    Matcher matcher = NEXT_LINK_PATTERN.matcher(linkHeader);
                    return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
                });
    }

    private Course parseCourse(JsonNode node) {
        return new Course(
                longValue(node, "id"),
                textValue(node, "name"),
                textValue(node, "course_code"),
                textValue(node, "workflow_state")
        );
    }

    private CanvasAssignment parseAssignment(JsonNode node, Long fallbackCourseId) {
        JsonNode submission = node.path("submission");
        boolean submitted = booleanValue(node, "has_submitted_submissions")
                || booleanValue(submission, "submitted")
                || textValue(submission, "workflow_state").equalsIgnoreCase("submitted");
        boolean missing = booleanValue(submission, "missing");

        return new CanvasAssignment(
                longValue(node, "id"),
                firstLongValue(node, "course_id", fallbackCourseId),
                textValue(node, "name"),
                cleanText(textValue(node, "description")),
                textValue(node, "html_url"),
                dateTimeValue(node, "due_at"),
                !node.has("published") || booleanValue(node, "published"),
                submitted,
                missing
        );
    }

    private CanvasTodoItem parseTodoItem(JsonNode node) {
        JsonNode assignment = node.path("assignment");
        Long assignmentId = firstLongValue(assignment, "id", longValue(node, "assignment_id"));
        Long courseId = firstLongValue(node, "course_id", longValue(assignment, "course_id"));
        boolean completed = booleanValue(assignment, "has_submitted_submissions")
                || booleanValue(node, "completed")
                || booleanValue(node, "ignore");

        return new CanvasTodoItem(
                assignmentId,
                courseId,
                firstTextValue(assignment, "name", textValue(node, "context_name")),
                firstTextValue(assignment, "html_url", textValue(node, "html_url")),
                firstDateTimeValue(assignment, "due_at", dateTimeValue(node, "end_at")),
                completed
        );
    }

    private CanvasPlannerItem parsePlannerItem(JsonNode node) {
        JsonNode plannable = node.path("plannable");
        JsonNode submissions = node.path("submissions");
        boolean completed = booleanValue(node, "completed")
                || booleanValue(node, "submitted")
                || booleanValue(submissions, "submitted")
                || booleanValue(submissions, "graded");

        return new CanvasPlannerItem(
                firstLongValue(node, "plannable_id", longValue(plannable, "id")),
                firstLongValue(node, "course_id", longValue(plannable, "course_id")),
                firstTextValue(plannable, "title", firstTextValue(plannable, "name", textValue(node, "title"))),
                firstTextValue(node, "html_url", textValue(plannable, "html_url")),
                firstDateTimeValue(plannable, "due_at", firstDateTimeValue(node, "plannable_date", dateTimeValue(node, "end_at"))),
                completed
        );
    }

    private URI resolve(String path) {
        return baseUri.resolve(path.startsWith("/") ? path.substring(1) : path);
    }

    private static URI normalizeBaseUri(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            throw new CanvasApiException(HttpStatus.BAD_REQUEST, "Canvas base URL is required.");
        }

        try {
            URI uri = URI.create(baseUrl.trim());
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("https") && !scheme.equalsIgnoreCase("http")) || uri.getHost() == null) {
                throw new IllegalArgumentException("Canvas URL must include http or https and a host.");
            }
            String normalized = uri.toString();
            if (!normalized.endsWith("/")) {
                normalized += "/";
            }
            return URI.create(normalized);
        } catch (IllegalArgumentException ex) {
            throw new CanvasApiException(HttpStatus.BAD_REQUEST, "Enter a valid Canvas URL, such as https://school.instructure.com.");
        }
    }

    private static String requireToken(String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            throw new CanvasApiException(HttpStatus.BAD_REQUEST, "Canvas access token is required.");
        }
        return accessToken.trim();
    }

    private static String encodePathSegment(Object value) {
        return URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8);
    }

    private static Long longValue(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (value.canConvertToLong()) {
            return value.asLong();
        }
        if (value.isTextual()) {
            try {
                return Long.parseLong(value.asText());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Long firstLongValue(JsonNode node, String fieldName, Long fallback) {
        Long value = longValue(node, fieldName);
        return value == null ? fallback : value;
    }

    private static String textValue(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return "";
        }
        return value.asText("");
    }

    private static String firstTextValue(JsonNode node, String fieldName, String fallback) {
        String value = textValue(node, fieldName);
        return StringUtils.hasText(value) ? value : fallback;
    }

    private static boolean booleanValue(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return false;
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        if (value.isNumber()) {
            return value.asInt() != 0;
        }
        return Boolean.parseBoolean(value.asText());
    }

    private static OffsetDateTime dateTimeValue(JsonNode node, String fieldName) {
        String rawValue = textValue(node, fieldName);
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }
        try {
            return OffsetDateTime.parse(rawValue);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static OffsetDateTime firstDateTimeValue(JsonNode node, String fieldName, OffsetDateTime fallback) {
        OffsetDateTime value = dateTimeValue(node, fieldName);
        return value == null ? fallback : value;
    }

    private static String cleanText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value
                .replaceAll("<[^>]+>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
