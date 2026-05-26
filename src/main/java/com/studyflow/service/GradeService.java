package com.studyflow.service;

import com.studyflow.dto.GradeRequest;
import com.studyflow.entity.Course;
import com.studyflow.entity.Grade;
import com.studyflow.exception.ResourceNotFoundException;
import com.studyflow.repository.CourseRepository;
import com.studyflow.repository.GradeRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GradeService {

    private final GradeRepository gradeRepository;
    private final CourseRepository courseRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public GradeService(
            GradeRepository gradeRepository,
            CourseRepository courseRepository,
            AuthenticatedUserService authenticatedUserService
    ) {
        this.gradeRepository = gradeRepository;
        this.courseRepository = courseRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    @Transactional(readOnly = true)
    public List<Grade> getGradesByCourse(Long courseId, String email) {
        Long userId = authenticatedUserService.getRequiredUserId(email);
        findCourseOrThrow(courseId, userId);
        return gradeRepository.findByCourseIdAndCourseUserId(courseId, userId);
    }

    @Transactional(readOnly = true)
    public Grade getGrade(Long id, String email) {
        Long userId = authenticatedUserService.getRequiredUserId(email);
        return findGradeOrThrow(id, userId);
    }

    @Transactional
    public Grade createGrade(Long courseId, GradeRequest request, String email) {
        Long userId = authenticatedUserService.getRequiredUserId(email);
        Course course = findCourseOrThrow(courseId, userId);

        Grade grade = new Grade(
                request.assignmentName(),
                request.score(),
                request.maxScore(),
                request.weight(),
                course
        );
        return gradeRepository.save(grade);
    }

    @Transactional
    public Grade updateGrade(Long id, GradeRequest request, String email) {
        Long userId = authenticatedUserService.getRequiredUserId(email);
        Grade grade = findGradeOrThrow(id, userId);
        grade.setTitle(request.assignmentName());
        grade.setScore(request.score());
        grade.setMaxScore(request.maxScore());
        grade.setWeight(request.weight());
        return gradeRepository.save(grade);
    }

    @Transactional
    public void deleteGrade(Long id, String email) {
        Long userId = authenticatedUserService.getRequiredUserId(email);
        Grade grade = findGradeOrThrow(id, userId);
        gradeRepository.delete(grade);
    }

    private Grade findGradeOrThrow(Long id, Long userId) {
        return gradeRepository.findByIdAndCourseUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Grade not found with id " + id));
    }

    private Course findCourseOrThrow(Long courseId, Long userId) {
        return courseRepository.findByIdAndUserId(courseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id " + courseId));
    }
}
