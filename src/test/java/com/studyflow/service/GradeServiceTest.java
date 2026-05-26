package com.studyflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studyflow.dto.GradeRequest;
import com.studyflow.entity.Course;
import com.studyflow.entity.Grade;
import com.studyflow.exception.ResourceNotFoundException;
import com.studyflow.repository.CourseRepository;
import com.studyflow.repository.GradeRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GradeServiceTest {

    @Mock
    private GradeRepository gradeRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private GradeService gradeService;

    @Test
    void createGradeSavesGradeForExistingCourse() {
        Course course = new Course();
        course.setId(2L);
        course.setName("Algorithms");
        GradeRequest request = new GradeRequest("Midterm", 92.0, 100.0, 30.0);

        when(courseRepository.findById(2L)).thenReturn(Optional.of(course));
        when(gradeRepository.save(any(Grade.class))).thenAnswer(invocation -> {
            Grade savedGrade = invocation.getArgument(0);
            savedGrade.setId(15L);
            return savedGrade;
        });

        Grade created = gradeService.createGrade(2L, request);

        assertEquals(15L, created.getId());
        assertEquals("Midterm", created.getTitle());
        assertEquals(92.0, created.getScore());
        assertEquals(100.0, created.getMaxScore());
        assertEquals(30.0, created.getWeight());
        assertEquals(course, created.getCourse());
    }

    @Test
    void createGradeThrowsWhenCourseDoesNotExist() {
        GradeRequest request = new GradeRequest("Midterm", 92.0, 100.0, 30.0);
        when(courseRepository.findById(404L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> gradeService.createGrade(404L, request)
        );

        assertTrue(exception.getMessage().contains("Course not found"));
    }

    @Test
    void getGradesByCourseRequiresExistingCourse() {
        Grade grade = new Grade();
        grade.setId(7L);
        grade.setTitle("Quiz 1");

        when(courseRepository.existsById(2L)).thenReturn(true);
        when(gradeRepository.findByCourseId(2L)).thenReturn(List.of(grade));

        List<Grade> grades = gradeService.getGradesByCourse(2L);

        assertEquals(1, grades.size());
        assertEquals("Quiz 1", grades.get(0).getTitle());
    }

    @Test
    void getGradeReturnsExistingGrade() {
        Grade grade = new Grade();
        grade.setId(8L);
        grade.setTitle("Final");
        when(gradeRepository.findById(8L)).thenReturn(Optional.of(grade));

        Grade found = gradeService.getGrade(8L);

        assertEquals("Final", found.getTitle());
    }

    @Test
    void updateGradeChangesEditableFields() {
        Grade grade = new Grade();
        grade.setId(8L);
        grade.setTitle("Old grade");
        grade.setScore(75.0);
        grade.setMaxScore(100.0);
        grade.setWeight(20.0);

        GradeRequest request = new GradeRequest("Updated grade", 88.0, 100.0, 25.0);
        when(gradeRepository.findById(8L)).thenReturn(Optional.of(grade));
        when(gradeRepository.save(grade)).thenReturn(grade);

        Grade updated = gradeService.updateGrade(8L, request);

        assertEquals("Updated grade", updated.getTitle());
        assertEquals(88.0, updated.getScore());
        assertEquals(100.0, updated.getMaxScore());
        assertEquals(25.0, updated.getWeight());
    }

    @Test
    void deleteGradeDeletesExistingGrade() {
        Grade grade = new Grade();
        grade.setId(8L);
        when(gradeRepository.findById(8L)).thenReturn(Optional.of(grade));

        gradeService.deleteGrade(8L);

        verify(gradeRepository).delete(grade);
    }
}
