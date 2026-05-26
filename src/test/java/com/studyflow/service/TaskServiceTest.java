package com.studyflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studyflow.dto.TaskRequest;
import com.studyflow.entity.Course;
import com.studyflow.entity.Priority;
import com.studyflow.entity.Task;
import com.studyflow.entity.TaskStatus;
import com.studyflow.exception.ResourceNotFoundException;
import com.studyflow.repository.CourseRepository;
import com.studyflow.repository.TaskRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.jpa.domain.Specification;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    void createTaskSavesTaskForExistingCourse() {
        Course course = new Course();
        course.setId(2L);
        course.setName("Algorithms");
        TaskRequest request = new TaskRequest(
                "Finish homework",
                "Complete dynamic programming exercises",
                LocalDate.now().plusDays(3),
                Priority.HIGH,
                TaskStatus.TODO
        );

        when(courseRepository.findById(2L)).thenReturn(Optional.of(course));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task savedTask = invocation.getArgument(0);
            savedTask.setId(20L);
            return savedTask;
        });

        Task created = taskService.createTask(2L, request);

        assertEquals(20L, created.getId());
        assertEquals("Finish homework", created.getTitle());
        assertEquals(Priority.HIGH, created.getPriority());
        assertEquals(TaskStatus.TODO, created.getStatus());
        assertEquals(course, created.getCourse());
    }

    @Test
    void createTaskThrowsWhenCourseDoesNotExist() {
        TaskRequest request = new TaskRequest(
                "Finish homework",
                "Complete dynamic programming exercises",
                LocalDate.now().plusDays(3),
                Priority.HIGH,
                TaskStatus.TODO
        );
        when(courseRepository.findById(404L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> taskService.createTask(404L, request)
        );

        assertTrue(exception.getMessage().contains("Course not found"));
    }

    @Test
    void getTasksByCourseRequiresExistingCourse() {
        Task task = new Task();
        task.setId(11L);
        task.setTitle("Read chapter 4");

        when(courseRepository.existsById(2L)).thenReturn(true);
        when(taskRepository.findByCourseId(2L)).thenReturn(List.of(task));

        List<Task> tasks = taskService.getTasksByCourse(2L);

        assertEquals(1, tasks.size());
        assertEquals("Read chapter 4", tasks.get(0).getTitle());
    }

    @Test
    void searchTasksAppliesFiltersAndValidatesCourse() {
        Task task = new Task();
        task.setId(11L);
        task.setTitle("Finish homework");
        task.setDueDate(LocalDate.now().plusDays(2));
        task.setPriority(Priority.HIGH);
        task.setStatus(TaskStatus.TODO);

        when(courseRepository.existsById(2L)).thenReturn(true);
        when(taskRepository.findAll(anyTaskSpecification())).thenReturn(List.of(task));

        List<Task> tasks = taskService.searchTasks(
                2L,
                TaskStatus.TODO,
                Priority.HIGH,
                "homework",
                "dueDate"
        );

        assertEquals(1, tasks.size());
        assertEquals("Finish homework", tasks.get(0).getTitle());
        verify(courseRepository).existsById(2L);
        verify(taskRepository).findAll(anyTaskSpecification());
    }

    @Test
    void searchTasksThrowsWhenCourseFilterDoesNotExist() {
        when(courseRepository.existsById(404L)).thenReturn(false);

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> taskService.searchTasks(404L, null, null, null, "dueDate")
        );

        assertTrue(exception.getMessage().contains("Course not found"));
    }

    @Test
    void searchTasksSortsByDueDateThenPriority() {
        Task laterHigh = task("Later high", LocalDate.now().plusDays(5), Priority.HIGH);
        Task earlierLow = task("Earlier low", LocalDate.now().plusDays(1), Priority.LOW);
        Task earlierHigh = task("Earlier high", LocalDate.now().plusDays(1), Priority.HIGH);
        when(taskRepository.findAll(anyTaskSpecification())).thenReturn(List.of(laterHigh, earlierLow, earlierHigh));

        List<Task> tasks = taskService.searchTasks(null, null, null, null, "dueDate");

        assertEquals("Earlier high", tasks.get(0).getTitle());
        assertEquals("Earlier low", tasks.get(1).getTitle());
        assertEquals("Later high", tasks.get(2).getTitle());
    }

    @Test
    void searchTasksSortsByPriorityThenDueDate() {
        Task low = task("Low", LocalDate.now().plusDays(1), Priority.LOW);
        Task highLater = task("High later", LocalDate.now().plusDays(5), Priority.HIGH);
        Task highSooner = task("High sooner", LocalDate.now().plusDays(2), Priority.HIGH);
        Task medium = task("Medium", LocalDate.now().plusDays(1), Priority.MEDIUM);
        when(taskRepository.findAll(anyTaskSpecification())).thenReturn(List.of(low, highLater, highSooner, medium));

        List<Task> tasks = taskService.searchTasks(null, null, null, null, "priority");

        assertEquals("High sooner", tasks.get(0).getTitle());
        assertEquals("High later", tasks.get(1).getTitle());
        assertEquals("Medium", tasks.get(2).getTitle());
        assertEquals("Low", tasks.get(3).getTitle());
    }

    @Test
    void updateTaskChangesEditableFields() {
        Task task = new Task();
        task.setId(11L);
        task.setTitle("Old task");
        task.setDescription("Old description");
        task.setDueDate(LocalDate.now().plusDays(1));
        task.setPriority(Priority.LOW);
        task.setStatus(TaskStatus.TODO);

        TaskRequest request = new TaskRequest(
                "Updated task",
                "Updated description",
                LocalDate.now().plusDays(5),
                Priority.MEDIUM,
                TaskStatus.IN_PROGRESS
        );
        when(taskRepository.findById(11L)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        Task updated = taskService.updateTask(11L, request);

        assertEquals("Updated task", updated.getTitle());
        assertEquals("Updated description", updated.getDescription());
        assertEquals(Priority.MEDIUM, updated.getPriority());
        assertEquals(TaskStatus.IN_PROGRESS, updated.getStatus());
    }

    @Test
    void deleteTaskDeletesExistingTask() {
        Task task = new Task();
        task.setId(11L);
        when(taskRepository.findById(11L)).thenReturn(Optional.of(task));

        taskService.deleteTask(11L);

        verify(taskRepository).delete(task);
    }

    private Task task(String title, LocalDate dueDate, Priority priority) {
        Task task = new Task();
        task.setTitle(title);
        task.setDueDate(dueDate);
        task.setPriority(priority);
        task.setStatus(TaskStatus.TODO);
        return task;
    }

    @SuppressWarnings("unchecked")
    private Specification<Task> anyTaskSpecification() {
        return any(Specification.class);
    }
}
