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

    public GradeService(GradeRepository gradeRepository, CourseRepository courseRepository) {
        this.gradeRepository = gradeRepository;
        this.courseRepository = courseRepository;
    }

    @Transactional(readOnly = true)
    public List<Grade> getGradesByCourse(Long courseId) {
        ensureCourseExists(courseId);
        return gradeRepository.findByCourseId(courseId);
    }

    @Transactional(readOnly = true)
    public Grade getGrade(Long id) {
        return findGradeOrThrow(id);
    }

    @Transactional
    public Grade createGrade(Long courseId, GradeRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id " + courseId));
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
    public Grade updateGrade(Long id, GradeRequest request) {
        Grade grade = findGradeOrThrow(id);
        grade.setTitle(request.assignmentName());
        grade.setScore(request.score());
        grade.setMaxScore(request.maxScore());
        grade.setWeight(request.weight());
        return gradeRepository.save(grade);
    }

    @Transactional
    public void deleteGrade(Long id) {
        Grade grade = findGradeOrThrow(id);
        gradeRepository.delete(grade);
    }

    private Grade findGradeOrThrow(Long id) {
        return gradeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grade not found with id " + id));
    }

    private void ensureCourseExists(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("Course not found with id " + courseId);
        }
    }
}
