package com.studyflow.repository;

import com.studyflow.entity.Grade;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GradeRepository extends JpaRepository<Grade, Long> {
    List<Grade> findByCourseId(Long courseId);
}
