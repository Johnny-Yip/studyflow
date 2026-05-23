package com.studyflow.repository;

import com.studyflow.entity.Task;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByCourseId(Long courseId);
}
