package com.example.spring.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.spring.dto.CourseDto;
import com.example.spring.service.CourseService;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@CrossOrigin(origins = "${cors.allowed-origins}") // 프론트엔드 포트
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    // 모든 강의
    @GetMapping
    public ResponseEntity<List<CourseDto>> getMajorCourses() {
        List<CourseDto> courses = courseService.getMajorCourses();
        return ResponseEntity.ok(courses);
    }

    // 특정 강의
    @GetMapping("/{id}")
    public ResponseEntity<CourseDto> getCourseById(@PathVariable String id) {
        CourseDto course = courseService.getCourseById(id);

        if (course == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();   // 404
        }
        return ResponseEntity.ok(course);     // 200
    }
}
