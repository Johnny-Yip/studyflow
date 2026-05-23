package com.studyflow.service;

import com.studyflow.dto.CourseRequest;
import com.studyflow.dto.CourseUpdateRequest;
import com.studyflow.entity.Course;
import com.studyflow.entity.User;
import com.studyflow.exception.ResourceNotFoundException;
import com.studyflow.repository.CourseRepository;
import com.studyflow.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public CourseService(CourseRepository courseRepository, UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Course> getCoursesByUser(Long userId) {
        ensureUserExists(userId);
        return courseRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Course getCourse(Long id) {
        return findCourseOrThrow(id);
    }

    @Transactional
    public Course createCourse(CourseRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + request.userId()));
        Course course = new Course(request.name(), request.description(), user);
        return courseRepository.save(course);
    }

    @Transactional
    public Course updateCourse(Long id, CourseUpdateRequest request) {
        Course course = findCourseOrThrow(id);
        course.setName(request.name());
        course.setDescription(request.description());
        return courseRepository.save(course);
    }

    @Transactional
    public void deleteCourse(Long id) {
        Course course = findCourseOrThrow(id);
        courseRepository.delete(course);
    }

    private Course findCourseOrThrow(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id " + id));
    }

    private void ensureUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id " + userId);
        }
    }
}
