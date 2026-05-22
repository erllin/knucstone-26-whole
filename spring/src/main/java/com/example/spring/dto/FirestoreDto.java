package com.example.spring.dto;
import java.util.List;
import java.util.Map;

public class FirestoreDto {
    /**
    * firestore에서 도큐먼트 단위로 가져오는 경우를 대비. (아마도 인공지능에 사용될 수도 있을 것 같네요.)
    * CourseDto, CreditTableDto, TrackDto는 Spring쪽 메모리에서 가져오면 될 것 같아요. (캐싱으로 처리해서..)
    * 읽어주셔서 감사합니다.
    */

    public record CourseDto(String id, String name, String ragContext, Map<String, Object> metadata) {}

    public record CreditTableDto(String department, int targetYear, int totalGraduationCredits,
                                 List<Map<String, Object>> details) {}

    public record TrackDto(int trackNo, String trackName, List<String> targetOccupations,
                           String completionGoal, List<Map<String, Object>> subjects) {}

    public record UserProfileDto(int admission, String department, int majorType, int nonRegularTerm,
                                 int regularTerm, boolean setupCompleted, String university) {}

    public record UserCourseDto(String category, String courseId, String courseName, int credits, String grade,
                                String id, boolean retake, String taken) {}

    public record UserSemesterDto(String id, int isNonRegularTerm, int sortKey, String taken, int term) {}
}
