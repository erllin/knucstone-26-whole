package com.example.spring.dto;

import java.util.List;

public record AiResponseDto(
        RecommendedTracks recommendedTracks,
        NextSemesterPlan nextSemesterPlan,
        TrackLearningSolution trackLearningSolution
) {
    public record RecommendedTracks(TrackDetail primaryTrack, TrackDetail secondaryTrack) {}
    public record TrackDetail(String trackName, String reason) {}
    public record NextSemesterPlan(List<CourseDetail> recommendedCourses) {}
    public record CourseDetail(String courseId, String courseName, String reason) {}

    public record TrackLearningSolution(List<String> trackGuides, List<String> actionPlans) {}
}
