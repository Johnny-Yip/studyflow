package com.studyflow.controller;

import com.studyflow.dto.CourseRequest;
import com.studyflow.dto.CourseResponse;
import com.studyflow.dto.CourseUpdateRequest;
import com.studyflow.service.CourseService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public List<CourseResponse> getCourses(Authentication authentication) {
        return courseService.getCoursesForUser(authentication.getName()).stream()
                .map(CourseResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public CourseResponse getCourse(@PathVariable Long id, Authentication authentication) {
        return CourseResponse.from(courseService.getCourse(id, authentication.getName()));
    }

    @PostMapping
    public ResponseEntity<CourseResponse> createCourse(
            @Valid @RequestBody CourseRequest request,
            Authentication authentication
    ) {
        CourseResponse response = CourseResponse.from(courseService.createCourse(request, authentication.getName()));
        return ResponseEntity
                .created(URI.create("/api/courses/" + response.id()))
                .body(response);
    }

    @PutMapping("/{id}")
    public CourseResponse updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody CourseUpdateRequest request,
            Authentication authentication
    ) {
        return CourseResponse.from(courseService.updateCourse(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id, Authentication authentication) {
        courseService.deleteCourse(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
