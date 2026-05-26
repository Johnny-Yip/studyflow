package com.studyflow.dto;

import com.studyflow.entity.User;

public record AuthResponse(
        String token,
        String tokenType,
        Long userId,
        String name,
        String email
) {
    public static AuthResponse from(User user, String token) {
        return new AuthResponse(
                token,
                "Bearer",
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }
}
