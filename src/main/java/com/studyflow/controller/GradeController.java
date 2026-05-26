package com.studyflow.controller;

import com.studyflow.dto.GradeRequest;
import com.studyflow.dto.GradeResponse;
import com.studyflow.service.GradeService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GradeController {

    private final GradeService gradeService;

    public GradeController(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    @GetMapping("/api/courses/{courseId}/grades")
    public List<GradeResponse> getGradesByCourse(@PathVariable Long courseId, Authentication authentication) {
        return gradeService.getGradesByCourse(courseId, authentication.getName()).stream()
                .map(GradeResponse::from)
                .toList();
    }

    @PostMapping("/api/courses/{courseId}/grades")
    public ResponseEntity<GradeResponse> createGrade(
            @PathVariable Long courseId,
            @Valid @RequestBody GradeRequest request,
            Authentication authentication
    ) {
        GradeResponse response = GradeResponse.from(gradeService.createGrade(courseId, request, authentication.getName()));
        return ResponseEntity
                .created(URI.create("/api/grades/" + response.id()))
                .body(response);
    }

    @GetMapping("/api/grades/{id}")
    public GradeResponse getGrade(@PathVariable Long id, Authentication authentication) {
        return GradeResponse.from(gradeService.getGrade(id, authentication.getName()));
    }

    @PutMapping("/api/grades/{id}")
    public GradeResponse updateGrade(
            @PathVariable Long id,
            @Valid @RequestBody GradeRequest request,
            Authentication authentication
    ) {
        return GradeResponse.from(gradeService.updateGrade(id, request, authentication.getName()));
    }

    @DeleteMapping("/api/grades/{id}")
    public ResponseEntity<Void> deleteGrade(@PathVariable Long id, Authentication authentication) {
        gradeService.deleteGrade(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
