package com.studyflow.canvas.dto;

import jakarta.validation.constraints.Size;

public record CanvasSettingsRequest(
        @Size(max = 500, message = "Canvas URL must be at most 500 characters")
        String baseUrl,

        @Size(max = 500, message = "Canvas token must be at most 500 characters")
        String accessToken
) {
}
