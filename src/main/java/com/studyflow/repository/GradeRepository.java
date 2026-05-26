package com.studyflow.repository;

import com.studyflow.entity.Grade;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GradeRepository extends JpaRepository<Grade, Long> {
    List<Grade> findByCourseId(Long courseId);

    List<Grade> findByCourseIdAndCourseUserId(Long courseId, Long userId);

    Optional<Grade> findByIdAndCourseUserId(Long id, Long userId);
}
