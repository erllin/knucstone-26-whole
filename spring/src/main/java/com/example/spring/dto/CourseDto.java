package com.example.spring.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CourseDto {
    private String id;
    private String name;
    private String ragContext;
    private Metadata metadata;

    public CourseDto() {}

    // Getter/Setter (lombok)
    @Getter
    @Setter
    public static class Metadata {
        private String yearSemester;
        private String category;
        private int credits;
        private List<String> tracks;
        private List<String> prerequisites;

        public Metadata() {}
    }
}
