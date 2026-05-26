package com.studyflow.service;

import com.studyflow.dto.DashboardSummaryResponse;
import com.studyflow.entity.TaskStatus;
import com.studyflow.repository.CourseRepository;
import com.studyflow.repository.TaskRepository;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final CourseRepository courseRepository;
    private final TaskRepository taskRepository;
    private final Clock clock;

    public DashboardService(CourseRepository courseRepository, TaskRepository taskRepository) {
        this(courseRepository, taskRepository, Clock.systemDefaultZone());
    }

    DashboardService(CourseRepository courseRepository, TaskRepository taskRepository, Clock clock) {
        this.courseRepository = courseRepository;
        this.taskRepository = taskRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        long totalCourses = courseRepository.count();
        long totalTasks = taskRepository.count();
        long completedTasks = taskRepository.countByStatus(TaskStatus.DONE);
        long openTasks = totalTasks - completedTasks;
        long overdueTasks = taskRepository.countByDueDateBeforeAndStatusNot(LocalDate.now(clock), TaskStatus.DONE);
        double completionPercentage = totalTasks == 0 ? 0 : (completedTasks * 100.0) / totalTasks;

        return new DashboardSummaryResponse(
                totalCourses,
                totalTasks,
                completedTasks,
                openTasks,
                overdueTasks,
                completionPercentage
        );
    }
}
