package com.studyflow.canvas.model;

public enum CanvasSource {
    ASSIGNMENT_API("Assignment API"),
    TODO_API("Todo API"),
    PLANNER_API("Planner API");

    private final String label;

    CanvasSource(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
