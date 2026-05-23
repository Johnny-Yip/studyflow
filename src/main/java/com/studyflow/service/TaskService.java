package com.studyflow.service;

import com.studyflow.dto.TaskRequest;
import com.studyflow.entity.Course;
import com.studyflow.entity.Task;
import com.studyflow.exception.ResourceNotFoundException;
import com.studyflow.repository.CourseRepository;
import com.studyflow.repository.TaskRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
