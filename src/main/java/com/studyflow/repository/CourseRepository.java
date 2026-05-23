package com.studyflow.repository;

import com.studyflow.entity.Course;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByUserId(Long userId);
}
