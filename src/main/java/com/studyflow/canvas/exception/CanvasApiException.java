package com.studyflow.canvas.exception;

import org.springframework.http.HttpStatus;

public class CanvasApiException extends RuntimeException {

    private final HttpStatus status;

    public CanvasApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
