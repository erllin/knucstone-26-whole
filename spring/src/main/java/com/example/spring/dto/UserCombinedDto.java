package com.example.spring.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.Map;

/**
 *  이 부분은 firebase의 users/{uid} 의 구조와 일치합니다.
 *  유저 데이터 구조입니다.
 */

@Getter
@Setter
public class UserCombinedDto {
    private Map<String, Object> userProfile;
    private List<Map<String, Object>> userCourses;
    private List<Map<String, Object>> userSemesters;
}
