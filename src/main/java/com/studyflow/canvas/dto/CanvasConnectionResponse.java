package com.studyflow.canvas.dto;

public record CanvasConnectionResponse(
        boolean connected,
        String message,
        int courseCount
) {
}
