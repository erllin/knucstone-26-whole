package com.example.spring.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.Map;

    /**
     *  이 부분은 학기(userSemester)랑 하위 강의(userCourse; taken값 일치)를
     *  연계해 처리하기 위한 구조입니다.
     *  taken이 oldTaken과 activeTaken으로 나뉘어 있는데,
     *  이 부분은, 학기 데이터가 변동될 시점에 taken: oldTaken -> activeTaken으로 변합니다.
     *  localCourses의 taken값도 activeTaken으로 연쇄적으로 같이 변화됩니다.
     *
     *  또한 localCourses는, 학기의 삭제가 발생했을 때, 연쇄적으로 삭제를 담당하기도 합니다.
     */


@Getter
@Setter
public class CourseSaveRequestDto {
    private String activeTaken;
    private int sortKey;
    private String targetSemId; // 기존 학기면 ID가 있고, 신규 학기면 null 또는 빈값
    private int isNonRegularTerm;
    private int term;
    private String oldTaken;    // 기존 학기명 (taken 갱신시 사용)
    private List<Map<String, Object>> localCourses; // 화면단에서 정리된 과목 리스트
}
