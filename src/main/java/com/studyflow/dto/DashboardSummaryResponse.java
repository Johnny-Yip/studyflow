package com.studyflow.dto;

public record DashboardSummaryResponse(
        long totalCourses,
        long totalTasks,
        long completedTasks,
        long openTasks,
        long overdueTasks,
        double completionPercentage
) {
}
