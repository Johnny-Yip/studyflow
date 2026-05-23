package com.studyflow.controller;

import com.studyflow.dto.CourseRequest;
import com.studyflow.dto.CourseResponse;
import com.studyflow.dto.CourseUpdateRequest;
import com.studyflow.service.CourseService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public List<CourseResponse> getCourses(@RequestParam(required = false) Long userId) {
        if (userId != null) {
            return courseService.getCoursesByUser(userId).stream()
                    .map(CourseResponse::from)
                    .toList();
        }
        return courseService.getAllCourses().stream()
                .map(CourseResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public CourseResponse getCourse(@PathVariable Long id) {
        return CourseResponse.from(courseService.getCourse(id));
    }

    @PostMapping
    public ResponseEntity<CourseResponse> createCourse(@Valid @RequestBody CourseRequest request) {
        CourseResponse response = CourseResponse.from(courseService.createCourse(request));
        return ResponseEntity
                .created(URI.create("/api/courses/" + response.id()))
                .body(response);
    }

    @PutMapping("/{id}")
    public CourseResponse updateCourse(@PathVariable Long id, @Valid @RequestBody CourseUpdateRequest request) {
        return CourseResponse.from(courseService.updateCourse(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }
}
