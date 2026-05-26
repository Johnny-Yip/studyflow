package com.studyflow.service;

import com.studyflow.dto.DashboardSummaryResponse;
import com.studyflow.entity.TaskStatus;
import com.studyflow.repository.CourseRepository;
import com.studyflow.repository.TaskRepository;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final CourseRepository courseRepository;
    private final TaskRepository taskRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final Clock clock;

    @Autowired
    public DashboardService(
            CourseRepository courseRepository,
            TaskRepository taskRepository,
            AuthenticatedUserService authenticatedUserService
    ) {
        this(courseRepository, taskRepository, authenticatedUserService, Clock.systemDefaultZone());
    }

    DashboardService(
            CourseRepository courseRepository,
            TaskRepository taskRepository,
            AuthenticatedUserService authenticatedUserService,
            Clock clock
    ) {
        this.courseRepository = courseRepository;
        this.taskRepository = taskRepository;
        this.authenticatedUserService = authenticatedUserService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(String email) {
        Long userId = authenticatedUserService.getRequiredUserId(email);

        long totalCourses = courseRepository.countByUserId(userId);
        long totalTasks = taskRepository.countByCourseUserId(userId);
        long completedTasks = taskRepository.countByCourseUserIdAndStatus(userId, TaskStatus.DONE);
        long openTasks = totalTasks - completedTasks;
        long overdueTasks = taskRepository.countByCourseUserIdAndDueDateBeforeAndStatusNot(
                userId,
                LocalDate.now(clock),
                TaskStatus.DONE
        );
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
