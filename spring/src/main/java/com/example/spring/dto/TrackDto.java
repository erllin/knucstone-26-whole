package com.example.spring.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

// 정규 트랙 데이터 구조입니다.

@Getter
@Setter
public class TrackDto {
    private int trackNo;
    private String trackName;
    private List<String> targetOccupations;
    private String completionGoal;
    private List<Subject> subjects;

    public TrackDto() {}

    @Getter
    @Setter
    public static class Subject {
        private int level;
        private String type;
        private int year;
        private int semester;
        private String id;
        private String name;

        public Subject() {}
    }
}
