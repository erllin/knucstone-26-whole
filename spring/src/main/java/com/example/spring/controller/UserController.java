package com.example.spring.controller;

import com.example.spring.dto.UserCombinedDto;
import com.example.spring.dto.CourseSaveRequestDto;
import com.example.spring.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "${cors.allowed-origins}") // 프론트엔드 포트
public class UserController {
    /**
     * axios랑 연동하면서 문제 없는 것은 확인했는데,...
     * google.ai 이용해서 ResponseEntity 붙였습니다.
     */
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // 유저 데이터 (학사정보) 가져오는 메소드
    @GetMapping("/users/{uid}")
    public ResponseEntity<UserCombinedDto> getUserData(@PathVariable String uid) throws Exception {
        try {
            UserCombinedDto userData = userService.getUserCombinedData(uid);
            if (userData == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 404
            }
            return ResponseEntity.ok(userData); //200

        } catch (Exception error) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); //500
        }

    }

    // 학사정보 업데이트
    @PutMapping("/users/{uid}/profile")
    public ResponseEntity<Map<String, String>> updateProfile(@PathVariable String uid,
                                                             @RequestBody Map<String, Object> data) {
        try {
            userService.updateProfile(uid, data);
            return ResponseEntity.ok(Map.of("message", "프로필 수정 성공")); // 200
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); // 400
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "프로필 수정 실패"));
        }
    }

    // 강의 추가
    @PostMapping("/users/{uid}/courses/{cid}")
    public ResponseEntity<Map<String, String>> addCourse(@PathVariable String uid, @PathVariable String cid,
                                                         @RequestBody Map<String, Object> courseData) {
        try {
            userService.addCourse(uid, cid, courseData);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "교과목 추가 성공")); // 201
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500
        }
    }

    // 강의 삭제 (현재는 재수강 부분에서 이용하고 있습니다.)
    @DeleteMapping("/users/{uid}/courses/{cid}")
    public ResponseEntity<Void> deleteCourse(@PathVariable String uid, @PathVariable String cid) {
        try {
            userService.deleteCourse(uid, cid);
            return ResponseEntity.noContent().build(); // 204
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500
        }
    }

    // 학기 추가
    @PostMapping("/users/{uid}/semesters")
    public ResponseEntity<Map<String, String>> addSemester(@PathVariable String uid,
                                                           @RequestBody Map<String, Object> semesterData) {
        try {
            userService.addSemester(uid, semesterData);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "학기 추가 성공")); // 201
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500
        }
    }
    // 학기 삭제
    @DeleteMapping("/users/{uid}/semesters/{semId}")
    public ResponseEntity<Void> deleteSemester(@PathVariable String uid, @PathVariable String semId,
                                               @RequestParam String taken) {
        try {
            userService.deleteSemester(uid, semId, taken);
            return ResponseEntity.noContent().build(); // 204
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500
        }
    }

    // coursePopup과 관련 있습니다.
    @PostMapping("/users/{uid}/semesters/save-all")
    public ResponseEntity<Map<String, String>> saveSemesterAndCourses(@PathVariable String uid,
                                                                      @RequestBody CourseSaveRequestDto req) {
        try {
            userService.saveSemesterAndCourses(uid, req);
            return ResponseEntity.ok(Map.of("message", "전체 저장 성공")); // 200
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500
        }
    }

    // 학기 연차값 업데이트
    @PutMapping("/users/{uid}/semester-taken")
    public ResponseEntity<Map<String, String>> updateSemesterTaken(@PathVariable String uid,
                                                                   @RequestBody Map<String, String> body) {
        try {
            userService.updateSemesterTaken(uid, body.get("semId"), body.get("takenOld"), body.get("takenNew"));
            return ResponseEntity.ok(Map.of("message", "학기 정보 갱신 성공"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}