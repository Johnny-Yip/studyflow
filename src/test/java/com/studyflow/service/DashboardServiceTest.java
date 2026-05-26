package com.studyflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studyflow.dto.DashboardSummaryResponse;
import com.studyflow.entity.TaskStatus;
import com.studyflow.repository.CourseRepository;
import com.studyflow.repository.TaskRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-05-26T08:00:00Z"),
            ZoneOffset.UTC
    );

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(courseRepository, taskRepository, authenticatedUserService, FIXED_CLOCK);
    }

    @Test
    void getSummaryCalculatesTaskStatistics() {
        when(authenticatedUserService.getRequiredUserId("student@example.com")).thenReturn(1L);
        when(courseRepository.countByUserId(1L)).thenReturn(3L);
        when(taskRepository.countByCourseUserId(1L)).thenReturn(8L);
        when(taskRepository.countByCourseUserIdAndStatus(1L, TaskStatus.DONE)).thenReturn(5L);
        when(taskRepository.countByCourseUserIdAndDueDateBeforeAndStatusNot(1L, LocalDate.of(2026, 5, 26), TaskStatus.DONE))
                .thenReturn(2L);

        DashboardSummaryResponse summary = dashboardService.getSummary("student@example.com");

        assertEquals(3L, summary.totalCourses());
        assertEquals(8L, summary.totalTasks());
        assertEquals(5L, summary.completedTasks());
        assertEquals(3L, summary.openTasks());
        assertEquals(2L, summary.overdueTasks());
        assertEquals(62.5, summary.completionPercentage());
    }

    @Test
    void getSummaryReturnsZeroCompletionWhenThereAreNoTasks() {
        when(authenticatedUserService.getRequiredUserId("student@example.com")).thenReturn(1L);
        when(courseRepository.countByUserId(1L)).thenReturn(2L);
        when(taskRepository.countByCourseUserId(1L)).thenReturn(0L);
        when(taskRepository.countByCourseUserIdAndStatus(1L, TaskStatus.DONE)).thenReturn(0L);
        when(taskRepository.countByCourseUserIdAndDueDateBeforeAndStatusNot(1L, LocalDate.of(2026, 5, 26), TaskStatus.DONE))
                .thenReturn(0L);

        DashboardSummaryResponse summary = dashboardService.getSummary("student@example.com");

        assertEquals(2L, summary.totalCourses());
        assertEquals(0L, summary.totalTasks());
        assertEquals(0L, summary.completedTasks());
        assertEquals(0L, summary.openTasks());
        assertEquals(0L, summary.overdueTasks());
        assertEquals(0.0, summary.completionPercentage());
        verify(taskRepository)
                .countByCourseUserIdAndDueDateBeforeAndStatusNot(1L, LocalDate.of(2026, 5, 26), TaskStatus.DONE);
    }
}
