package com.studyflow.repository;

import com.studyflow.entity.Task;
import com.studyflow.entity.TaskStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByCourseId(Long courseId);

    long countByStatus(TaskStatus status);

    long countByDueDateBeforeAndStatusNot(LocalDate dueDate, TaskStatus status);
}
