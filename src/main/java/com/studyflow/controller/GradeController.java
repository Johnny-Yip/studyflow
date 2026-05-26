package com.studyflow.controller;

import com.studyflow.dto.GradeRequest;
import com.studyflow.dto.GradeResponse;
import com.studyflow.service.GradeService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
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
    public List<GradeResponse> getGradesByCourse(@PathVariable Long courseId) {
        return gradeService.getGradesByCourse(courseId).stream()
                .map(GradeResponse::from)
                .toList();
    }

    @PostMapping("/api/courses/{courseId}/grades")
    public ResponseEntity<GradeResponse> createGrade(
            @PathVariable Long courseId,
            @Valid @RequestBody GradeRequest request
    ) {
        GradeResponse response = GradeResponse.from(gradeService.createGrade(courseId, request));
        return ResponseEntity
                .created(URI.create("/api/grades/" + response.id()))
                .body(response);
    }

    @GetMapping("/api/grades/{id}")
    public GradeResponse getGrade(@PathVariable Long id) {
        return GradeResponse.from(gradeService.getGrade(id));
    }

    @PutMapping("/api/grades/{id}")
    public GradeResponse updateGrade(@PathVariable Long id, @Valid @RequestBody GradeRequest request) {
        return GradeResponse.from(gradeService.updateGrade(id, request));
    }

    @DeleteMapping("/api/grades/{id}")
    public ResponseEntity<Void> deleteGrade(@PathVariable Long id) {
        gradeService.deleteGrade(id);
        return ResponseEntity.noContent().build();
    }
}
