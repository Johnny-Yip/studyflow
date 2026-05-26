package com.studyflow.dto;

import com.studyflow.entity.Grade;

public record GradeResponse(
        Long id,
        String assignmentName,
        Double score,
        Double maxScore,
        Double weight,
        Double percentage,
        Double weightedScore,
        Long courseId,
        String courseName
) {
    public static GradeResponse from(Grade grade) {
        double percentage = grade.getMaxScore() == 0 ? 0 : (grade.getScore() / grade.getMaxScore()) * 100;
        double weightedScore = percentage * (grade.getWeight() / 100);

        return new GradeResponse(
                grade.getId(),
                grade.getTitle(),
                grade.getScore(),
                grade.getMaxScore(),
                grade.getWeight(),
                percentage,
                weightedScore,
                grade.getCourse().getId(),
                grade.getCourse().getName()
        );
    }
}
