package com.studyflow.controller;

import com.studyflow.canvas.CanvasSyncService;
import com.studyflow.canvas.dto.CanvasConnectionResponse;
import com.studyflow.canvas.dto.CanvasSettingsRequest;
import com.studyflow.canvas.model.CanvasTaskBucket;
import com.studyflow.canvas.model.StudyTask;
import com.studyflow.canvas.model.SyncLogEntry;
import com.studyflow.canvas.model.SyncResult;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CanvasController {

    private final CanvasSyncService canvasSyncService;

    public CanvasController(CanvasSyncService canvasSyncService) {
        this.canvasSyncService = canvasSyncService;
    }

    @PostMapping("/api/canvas/test")
    public CanvasConnectionResponse testConnection(@Valid @RequestBody CanvasSettingsRequest request) {
        return canvasSyncService.testConnection(request);
    }

    @PostMapping("/api/canvas/sync")
    public SyncResult sync(@Valid @RequestBody CanvasSettingsRequest request, Authentication authentication) {
        return canvasSyncService.sync(request, authentication.getName());
    }

    @PostMapping("/api/canvas/mock-sync")
    public SyncResult mockSync(Authentication authentication) {
        return canvasSyncService.syncMock(authentication.getName());
    }

    @GetMapping("/api/canvas/tasks")
    public List<StudyTask> getCanvasTasks(
            @RequestParam(required = false, defaultValue = "ALL") CanvasTaskBucket bucket,
            Authentication authentication
    ) {
        return canvasSyncService.findTasks(bucket, authentication.getName());
    }

    @GetMapping("/api/canvas/sync-logs")
    public List<SyncLogEntry> getSyncLogs(Authentication authentication) {
        return canvasSyncService.findSyncLogs(authentication.getName());
    }
}
