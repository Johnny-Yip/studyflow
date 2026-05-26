package com.studyflow.service;

import com.studyflow.dto.CourseRequest;
import com.studyflow.dto.CourseUpdateRequest;
import com.studyflow.entity.Course;
import com.studyflow.entity.User;
import com.studyflow.exception.ResourceNotFoundException;
import com.studyflow.repository.CourseRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public CourseService(CourseRepository courseRepository, AuthenticatedUserService authenticatedUserService) {
        this.courseRepository = courseRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    @Transactional(readOnly = true)
    public List<Course> getCoursesForUser(String email) {
        Long userId = authenticatedUserService.getRequiredUserId(email);
        return courseRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Course getCourse(Long id, String email) {
        Long userId = authenticatedUserService.getRequiredUserId(email);
        return findCourseOrThrow(id, userId);
    }

    @Transactional
    public Course createCourse(CourseRequest request, String email) {
        User user = authenticatedUserService.getRequiredUser(email);
        Course course = new Course(request.name(), request.description(), user);
        return courseRepository.save(course);
    }

    @Transactional
    public Course updateCourse(Long id, CourseUpdateRequest request, String email) {
        Long userId = authenticatedUserService.getRequiredUserId(email);
        Course course = findCourseOrThrow(id, userId);
        course.setName(request.name());
        course.setDescription(request.description());
        return courseRepository.save(course);
    }

    @Transactional
    public void deleteCourse(Long id, String email) {
        Long userId = authenticatedUserService.getRequiredUserId(email);
        Course course = findCourseOrThrow(id, userId);
        courseRepository.delete(course);
    }

    private Course findCourseOrThrow(Long id, Long userId) {
        return courseRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id " + id));
    }
}
