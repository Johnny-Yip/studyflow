package com.studyflow.repository;

import com.studyflow.entity.Task;
import com.studyflow.entity.TaskStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
    List<Task> findByCourseId(Long courseId);

    List<Task> findByCourseIdAndCourseUserId(Long courseId, Long userId);

    Optional<Task> findByIdAndCourseUserId(Long id, Long userId);

    long countByCourseUserId(Long userId);

    long countByStatus(TaskStatus status);

    long countByCourseUserIdAndStatus(Long userId, TaskStatus status);

    long countByDueDateBeforeAndStatusNot(LocalDate dueDate, TaskStatus status);

    long countByCourseUserIdAndDueDateBeforeAndStatusNot(Long userId, LocalDate dueDate, TaskStatus status);
}
