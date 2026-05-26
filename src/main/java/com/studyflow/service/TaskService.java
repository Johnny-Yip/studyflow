package com.studyflow.service;

import com.studyflow.dto.TaskRequest;
import com.studyflow.entity.Course;
import com.studyflow.entity.Priority;
import com.studyflow.entity.Task;
import com.studyflow.entity.TaskStatus;
import com.studyflow.exception.ResourceNotFoundException;
import com.studyflow.repository.CourseRepository;
import com.studyflow.repository.TaskRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final CourseRepository courseRepository;

    public TaskService(TaskRepository taskRepository, CourseRepository courseRepository) {
        this.taskRepository = taskRepository;
        this.courseRepository = courseRepository;
    }

    @Transactional(readOnly = true)
    public List<Task> getTasksByCourse(Long courseId) {
        ensureCourseExists(courseId);
        return taskRepository.findByCourseId(courseId);
    }

    @Transactional(readOnly = true)
    public List<Task> searchTasks(
            Long courseId,
            TaskStatus status,
            Priority priority,
            String title,
            String sort
    ) {
        if (courseId != null) {
            ensureCourseExists(courseId);
        }

        Specification<Task> specification = Specification.where(null);

        if (courseId != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("course").get("id"), courseId));
        }

        if (status != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("status"), status));
        }

        if (priority != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("priority"), priority));
        }

        if (StringUtils.hasText(title)) {
            String pattern = "%" + title.trim().toLowerCase() + "%";
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern));
        }

        return taskRepository.findAll(specification).stream()
                .sorted(taskComparator(sort))
                .toList();
    }

    @Transactional(readOnly = true)
    public Task getTask(Long id) {
        return findTaskOrThrow(id);
    }

    @Transactional
    public Task createTask(Long courseId, TaskRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id " + courseId));
        Task task = new Task(
                request.title(),
                request.description(),
                request.dueDate(),
                request.priority(),
                request.status(),
                course
        );
        return taskRepository.save(task);
    }

    @Transactional
    public Task updateTask(Long id, TaskRequest request) {
        Task task = findTaskOrThrow(id);
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setDueDate(request.dueDate());
        task.setPriority(request.priority());
        task.setStatus(request.status());
        return taskRepository.save(task);
    }

    @Transactional
    public void deleteTask(Long id) {
        Task task = findTaskOrThrow(id);
        taskRepository.delete(task);
    }

    private Task findTaskOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + id));
    }

    private void ensureCourseExists(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("Course not found with id " + courseId);
        }
    }

    private Comparator<Task> taskComparator(String sort) {
        if ("priority".equalsIgnoreCase(sort)) {
            return Comparator
                    .comparingInt((Task task) -> priorityRank(task.getPriority()))
                    .thenComparing(Task::getDueDate)
                    .thenComparing(Task::getTitle, String.CASE_INSENSITIVE_ORDER);
        }

        return Comparator
                .comparing(Task::getDueDate)
                .thenComparingInt(task -> priorityRank(task.getPriority()))
                .thenComparing(Task::getTitle, String.CASE_INSENSITIVE_ORDER);
    }

    private int priorityRank(Priority priority) {
        return switch (priority) {
            case HIGH -> 0;
            case MEDIUM -> 1;
            case LOW -> 2;
        };
    }
}
