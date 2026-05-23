package com.studyflow.dto;

import com.studyflow.entity.Course;

public record CourseResponse(
        Long id,
        String name,
        String description,
        Long userId,
        String userName
) {
    public static CourseResponse from(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getName(),
                course.getDescription(),
                course.getUser().getId(),
                course.getUser().getName()
        );
    }
}
