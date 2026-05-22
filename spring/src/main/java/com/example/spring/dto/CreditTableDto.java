package com.example.spring.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 이수학점 전체 정보를 가져오기 위한 데이터구조입니다.
 * 캐싱으로 변경하였습니다.
 * 감사합니다.
 */

@Getter
@Setter
public class CreditTableDto {
    private String department;
    private int targetYear;
    private int totalGraduationCredits;
    private Detail matchedDetails;

    @Getter @Setter
    public static class Detail {
        private int majorType; // 1, 2, 3, 4
        private String majorTypeString; // 전공유형 (문자)
        private GeneralEducation generalEducation;
        private Major major;
        private int freeElective;
        private String teachingProfession;
    }

    @Getter @Setter
    public static class GeneralEducation {
        private int basic;
        private int balanced;
        private int gShare;
        private int glocal;
        private int total;
    }

    @Getter @Setter
    public static class Major {
        private int required;
        private int elective;
        private int advanced;
        private int total;
    }
}
