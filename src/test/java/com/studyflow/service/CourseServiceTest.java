package com.studyflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studyflow.dto.CourseRequest;
import com.studyflow.dto.CourseUpdateRequest;
import com.studyflow.entity.Course;
import com.studyflow.entity.User;
import com.studyflow.exception.ResourceNotFoundException;
import com.studyflow.repository.CourseRepository;
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
    private AuthenticatedUserService authenticatedUserService;

    @InjectMocks
    private CourseService courseService;

    @Test
    void createCourseSavesCourseForAuthenticatedUser() {
        User user = new User("Demo Student", "student@example.com", "hashed");
        user.setId(1L);
        CourseRequest request = new CourseRequest("Algorithms", "Graph theory and complexity");

        when(authenticatedUserService.getRequiredUser("student@example.com")).thenReturn(user);
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course savedCourse = invocation.getArgument(0);
            savedCourse.setId(10L);
            return savedCourse;
        });

        Course created = courseService.createCourse(request, "student@example.com");

        assertEquals(10L, created.getId());
        assertEquals("Algorithms", created.getName());
        assertEquals("Graph theory and complexity", created.getDescription());
        assertEquals(user, created.getUser());
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    void getCoursesForUserReturnsUserCourses() {
        Course course = new Course();
        course.setId(7L);
        course.setName("Databases");

        when(authenticatedUserService.getRequiredUserId("student@example.com")).thenReturn(1L);
        when(courseRepository.findByUserId(1L)).thenReturn(List.of(course));

        List<Course> courses = courseService.getCoursesForUser("student@example.com");

        assertEquals(1, courses.size());
        assertEquals("Databases", courses.get(0).getName());
    }

    @Test
    void getCourseThrowsWhenCourseBelongsToAnotherUser() {
        when(authenticatedUserService.getRequiredUserId("student@example.com")).thenReturn(1L);
        when(courseRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> courseService.getCourse(3L, "student@example.com"));
    }

    @Test
    void updateCourseChangesEditableFields() {
        Course course = new Course();
        course.setId(3L);
        course.setName("Old Name");
        course.setDescription("Old Description");

        CourseUpdateRequest request = new CourseUpdateRequest("New Name", "New Description");
        when(authenticatedUserService.getRequiredUserId("student@example.com")).thenReturn(1L);
        when(courseRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.of(course));
        when(courseRepository.save(course)).thenReturn(course);

        Course updated = courseService.updateCourse(3L, request, "student@example.com");

        assertEquals("New Name", updated.getName());
        assertEquals("New Description", updated.getDescription());
    }

    @Test
    void deleteCourseDeletesExistingCourse() {
        Course course = new Course();
        course.setId(3L);

        when(authenticatedUserService.getRequiredUserId("student@example.com")).thenReturn(1L);
        when(courseRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.of(course));

        courseService.deleteCourse(3L, "student@example.com");

        verify(courseRepository).delete(course);
    }
}
