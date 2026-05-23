package com.studyflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studyflow.dto.CourseRequest;
import com.studyflow.dto.CourseUpdateRequest;
import com.studyflow.entity.Course;
import com.studyflow.entity.User;
import com.studyflow.exception.ResourceNotFoundException;
import com.studyflow.repository.CourseRepository;
import com.studyflow.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CourseService courseService;

    @Test
    void createCourseSavesCourseForExistingUser() {
        User user = new User("Demo Student", "student@example.com");
        user.setId(1L);
        CourseRequest request = new CourseRequest("Algorithms", "Graph theory and complexity", 1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course savedCourse = invocation.getArgument(0);
            savedCourse.setId(10L);
            return savedCourse;
        });

        Course created = courseService.createCourse(request);

        assertEquals(10L, created.getId());
        assertEquals("Algorithms", created.getName());
        assertEquals("Graph theory and complexity", created.getDescription());
        assertEquals(user, created.getUser());
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    void createCourseThrowsWhenUserDoesNotExist() {
        CourseRequest request = new CourseRequest("Algorithms", "Graph theory and complexity", 99L);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> courseService.createCourse(request)
        );

        assertTrue(exception.getMessage().contains("User not found"));
    }

    @Test
    void getCoursesByUserRequiresExistingUser() {
        Course course = new Course();
        course.setId(7L);
        course.setName("Databases");

        when(userRepository.existsById(1L)).thenReturn(true);
        when(courseRepository.findByUserId(1L)).thenReturn(List.of(course));

        List<Course> courses = courseService.getCoursesByUser(1L);

        assertEquals(1, courses.size());
        assertEquals("Databases", courses.get(0).getName());
    }

    @Test
    void updateCourseChangesEditableFields() {
        Course course = new Course();
        course.setId(3L);
        course.setName("Old Name");
        course.setDescription("Old Description");

        CourseUpdateRequest request = new CourseUpdateRequest("New Name", "New Description");
        when(courseRepository.findById(3L)).thenReturn(Optional.of(course));
        when(courseRepository.save(course)).thenReturn(course);

        Course updated = courseService.updateCourse(3L, request);

        assertEquals("New Name", updated.getName());
        assertEquals("New Description", updated.getDescription());
    }

    @Test
    void deleteCourseDeletesExistingCourse() {
        Course course = new Course();
        course.setId(3L);
        when(courseRepository.findById(3L)).thenReturn(Optional.of(course));

        courseService.deleteCourse(3L);

        verify(courseRepository).delete(course);
    }
}
