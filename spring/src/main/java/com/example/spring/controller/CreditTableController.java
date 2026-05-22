package com.example.spring.controller;

import com.example.spring.service.CreditTableService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class CreditTableController {

    private final CreditTableService creditTableService;

    public CreditTableController(CreditTableService creditTableService) {
        this.creditTableService = creditTableService;
    }

    /**
     * 유저 학적 정보(학과, 전공유형 기반으로) 이수 학점 요건표 조회하는 부분이에요.
     * 호출 주소 규격: GET /api/users/{uid}/credit_tables/{documentId}
     */
    @GetMapping("/users/{uid}/credit_tables/{documentId}")
    public ResponseEntity<Map<String, Object>> getMyCustomCreditTable(
            @PathVariable("uid") String uid,
            @PathVariable("documentId") String documentId) {
        try {
            Map<String, Object> result = creditTableService.getUserMatchedCreditTable(uid, documentId);
            return ResponseEntity.ok(result);   // 200

        } catch (IllegalArgumentException e) {
            // 학과명-학점표 바인딩 실패: 400
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // 파이어베이스 트랜잭션 에러 등 서버 예외: 500
            return ResponseEntity.status(500).body(Map.of("error", "서버 내부 학점표 연산 실패: " + e.getMessage()));
        }
    }
}
