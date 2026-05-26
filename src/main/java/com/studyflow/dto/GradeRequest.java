package com.studyflow.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GradeRequest(
        @NotBlank(message = "Assignment name is required")
        @Size(max = 160, message = "Assignment name must be at most 160 characters")
        String assignmentName,

        @NotNull(message = "Score is required")
        @DecimalMin(value = "0.0", message = "Score must be zero or greater")
        Double score,

        @NotNull(message = "Max score is required")
        @DecimalMin(value = "0.01", message = "Max score must be greater than zero")
        Double maxScore,

        @NotNull(message = "Weight is required")
        @DecimalMin(value = "0.0", message = "Weight must be zero or greater")
        @DecimalMax(value = "100.0", message = "Weight must be at most 100")
        Double weight
) {
    @AssertTrue(message = "Score must be less than or equal to max score")
    public boolean isScoreWithinMaxScore() {
        return score == null || maxScore == null || score <= maxScore;
    }
}
