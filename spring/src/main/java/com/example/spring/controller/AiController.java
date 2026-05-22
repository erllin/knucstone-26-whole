package com.example.spring.controller;

import com.example.spring.dto.AiResponseDto;
import com.example.spring.service.AiService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    /**
     * React(Axios)에서 호출하는 AI 리포트 생성 API
     * SecurityFilter를 거쳐 검증 완료된 Firebase UID를 주체(Principal)로 안전하게 꺼내 사용합니다.
     */
    @GetMapping("/consulting-report")
    public ResponseEntity<AiResponseDto> getConsultingReport(@RequestParam("creditTableDocId") String creditTableDocId) {
        try {
            // FirebaseTokenFilter에서 세션에 저장한 uid 추출
            String uid = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

            AiResponseDto report = aiService.getAcademicConsultingReport(uid, creditTableDocId);
            return ResponseEntity.ok(report);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
