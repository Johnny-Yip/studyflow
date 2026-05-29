package com.studyflow.canvas.model;

public record Course(
        Long id,
        String name,
        String courseCode,
        String workflowState
) {
    public boolean isActive() {
        if (id == null || name == null || name.isBlank()) {
            return false;
        }

        if (workflowState == null || workflowState.isBlank()) {
            return true;
        }

        String normalizedState = workflowState.trim().toLowerCase();
        return !normalizedState.equals("deleted")
                && !normalizedState.equals("completed")
                && !normalizedState.equals("unpublished");
    }
}
