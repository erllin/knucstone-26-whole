package com.example.spring.service;

import com.example.spring.dto.*;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiService {

    private final ChatModel chatModel;
    private final UserService userService;
    private final CourseService courseService;
    private final CreditTableService creditTableService;
    private final TrackService trackService;
    private final ObjectMapper objectMapper;

    public AiService(ChatModel chatModel, UserService userService, CourseService courseService,
                     CreditTableService creditTableService, TrackService trackService, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.userService = userService;
        this.courseService = courseService;
        this.creditTableService = creditTableService;
        this.trackService = trackService;
        this.objectMapper = objectMapper;
    }

    public AiResponseDto getAcademicConsultingReport(String uid, String creditTableDocId) throws Exception {
        UserCombinedDto userCombinedData = userService.getUserCombinedData(uid);
        Map<String, Object> userProfile = (Map<String, Object>) userCombinedData.getUserProfile();

        // AI 시간 제한 -> 기존 리포트 반환
        AiResponseDto lastAiReport = validAiCooldown(userProfile, 180);
        if (lastAiReport != null) { return lastAiReport; }

        Map<String, Object> matchedCreditTable = creditTableService.getUserMatchedCreditTable(uid, creditTableDocId);
        List<TrackDto> tracks = trackService.getTracks();
        List<CourseDto> availableCourses = courseService.getMajorCourses();

        // 프롬프트에 넣을 사용자 학사정보
        Map<String, Object> lightUserData = new HashMap<>();
        // 이수학기만 남기고 모두 제거.
        if (userCombinedData.getUserProfile() != null) {
            lightUserData.put("regularTerm", userCombinedData.getUserProfile().get("regularTerm"));
        }
        // 수강이력에서 (전선, 전필)과목만 필터하고, (교과목번호, 교과목명, 성적) 남기고 모두 필터.
        List<Map<String, Object>> lightUserCourses = Optional.ofNullable(userCombinedData.getUserCourses())
                .orElse(Collections.emptyList()).stream()
                .filter(courseMap -> {
                    Object categoryObj = courseMap.get("category");
                    if (categoryObj == null) return false;
                    String category = categoryObj.toString().trim();
                    // '전선' 또는 '전필'인 전공 과목만 남기고 모두 필터. (AI의 판단에 영향을 줄 수 있는 것들 제거.)
                    return "전선".equals(category) || "전필".equals(category);
                })
                .map(courseMap -> {
                    Map<String, Object> cleanCourse = new HashMap<>();
                    // 교과목번호, 과목명, 성적만 남기고 모두 필터.
                    cleanCourse.put("courseId", courseMap.get("courseId"));
                    cleanCourse.put("courseName", courseMap.get("courseName"));
                    cleanCourse.put("grade", courseMap.get("grade"));
                    return cleanCourse;
                })
                .toList();
        // 합치기.
        lightUserData.put("completedCourses", lightUserCourses);


        // 트랙 데이터에서 subject 부분, 교과목번호만 남기고 모두 삭제.
        List<Map<String, Object>> lightTracks = tracks.stream()
                .map(track -> {
                    Map<String, Object> minTrack = new HashMap<>();

                    // 트랙의 핵심 속성들은 그대로 남김. (트랙번호, 이름, 진로, 목표)
                    minTrack.put("trackNo", track.getTrackNo());
                    minTrack.put("trackName", track.getTrackName());
                    minTrack.put("targetOccupations", track.getTargetOccupations());
                    minTrack.put("completionGoal", track.getCompletionGoal());

                    // subjects 부분, 교과목번호만 남기고 모두 필터 (추후 AI가 간단하게 연산, 대조할 수 있게끔.)
                    List<String> subjectIds = Optional.ofNullable(track.getSubjects())
                            .orElse(Collections.emptyList()).stream()
                            .map(TrackDto.Subject::getId)
                            .filter(Objects::nonNull)
                            .map(String::trim)
                            .toList();

                    minTrack.put("subjectIds", subjectIds);
                    return minTrack;
                })
                .toList();

        // 전공 과목 리스트에서 이수 과목 필터링. (이 부분은, 그럴일은 없지만, 혹시나를 위해 원본 userCombined 데이터로 필터링함.)
        Set<String> completedCourseIds = Optional.ofNullable(userCombinedData.getUserCourses())
                .orElse(Collections.emptyList()).stream()
                .map(courseMap -> {
                    Object courseIdObj = courseMap.get("courseId");
                    return (courseIdObj != null) ? courseIdObj.toString().trim() : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 추가 필터링. (다음 학기만, 전선만 남기기 => 입력 토큰과 연산의 최소화. 전필은 필수이므로 연산보다는 프론트에 표시만 해두는 것이?)
        int regularTerm = Integer.parseInt(userProfile.get("regularTerm").toString());

        List<CourseDto> filteredCourses = Optional.ofNullable(availableCourses)
                .orElse(Collections.emptyList()).stream()
                .filter(course -> course.getId() != null)
                // I - 우선 이수한 과목 원천 배제
                .filter(course -> !completedCourseIds.contains(course.getId().trim()))
                // II - 걔중 '전선'인 과목만 남기기 (다만, 학과 전공교과목이 들어와서 대부분이 전선과목이라 그리 크지 않을 듯.)
                .filter(course -> course.getMetadata() != null && "전선".equals(course.getMetadata().getCategory()))
                // III - 다음 학기 과목만 남기게끔.
                .filter(course -> {
                    String ys = course.getMetadata().getYearSemester(); // 예: "1/1학기", "2/2학기", "4/전학기"
                    if (ys == null) return false;

                    String targetYearPrefix;    // 대상 학년 (ex: "2/")
                    String targetSemesterSuffix;// 대상 학기 (ex: "2학기")

                    if (regularTerm == -1)      { targetYearPrefix = "1/"; targetSemesterSuffix = "1학기"; }
                    else if (regularTerm == 0)  { targetYearPrefix = "1/"; targetSemesterSuffix = "2학기"; }
                    else if (regularTerm == 1)  { targetYearPrefix = "2/"; targetSemesterSuffix = "1학기"; }
                    else if (regularTerm == 2)  { targetYearPrefix = "2/"; targetSemesterSuffix = "2학기"; }
                    else if (regularTerm == 3)  { targetYearPrefix = "3/"; targetSemesterSuffix = "1학기"; }
                    else if (regularTerm == 4)  { targetYearPrefix = "3/"; targetSemesterSuffix = "2학기"; }
                    else if (regularTerm == 5)  { targetYearPrefix = "4/"; targetSemesterSuffix = "1학기"; }
                    else if (regularTerm == 6)  { targetYearPrefix = "4/"; targetSemesterSuffix = "2학기"; }
                    else { return false; }

                    // 학년 일치하는지?
                    if (!ys.startsWith(targetYearPrefix)) { return false; }

                    // 학기를 만족하거나 '전학기'인 과목만 남김
                    return ys.endsWith(targetSemesterSuffix) || ys.endsWith("전학기");
                })
                .toList();

        // id, name, ragContext, track 정보만 분리해서 조금 더 가볍게. (판단에 필요한 데이터들만)
        List<Map<String, Object>> lightFilteredCourses = filteredCourses.stream()
                .map(course -> {
                    Map<String, Object> minCourse = new HashMap<>();

                    // 교과목번호, 과목명
                    minCourse.put("courseId", course.getId());
                    minCourse.put("courseName", course.getName());

                    // ragContext 추출하기 (추천 사유나, 과목 추천하는데 쓰일 수 있다.)
                    minCourse.put("ragContext", course.getRagContext());

                    // 트랙 연계성 분석을 위해 metadata 내의 tracks 배열만 쏙 빼서 추가 (정규트랙과는 다르나, 보조 지표로 가능할 듯)
                    if (course.getMetadata() != null && course.getMetadata().getTracks() != null) {
                        minCourse.put("associatedTracks", course.getMetadata().getTracks());
                    }

                    return minCourse;
                })
                .toList();

        // 프롬프트에 넣기 위해 하나의 JSON으로 통합/변환
        Map<String, Object> promptContextMap = new HashMap<>();
        promptContextMap.put("USER_DATA", lightUserData);
        promptContextMap.put("GRADUATION_REQUIREMENTS", matchedCreditTable);
        promptContextMap.put("CURRICULUM_TRACKS", lightTracks);
        promptContextMap.put("AVAILABLE_COURSES", lightFilteredCourses);

        String rawContextJson = objectMapper.writeValueAsString(promptContextMap);

        // 프롬프트 (한국어에서 영문으로 변경되었어요.) >> 출력은 한국어로 올바르게 나오는 것 같아요. (하루 출력 제한 때문에 최소화함.)
        String systemPromptText = getSystemPrompt();

        // AI에 위의 텍스트 명령구와 데이터 둘 다 주입
        SystemMessage systemMessage = new SystemMessage(systemPromptText);
        UserMessage userMessage = new UserMessage("분석 대상 데이터:\n" + rawContextJson);
        // AI 호출 결과부
        ChatResponse response = null;

        // 한도 넘어가면 모델 변경해주는 파트 (Gemini-2.5-Fl-li > Gemini-2.5-Fl > 목업용 데이터)
        // 각 모델 당 하루 20회분 가능(한데, 데이터 양 커져서 토큰 많아지면 20회 미만이 될 수도...)
        try {
            // Gemini-2.5-Flash-Light (application.properties 부분에 위치)
            Prompt primaryPrompt = new Prompt(List.of(systemMessage, userMessage));
            response = chatModel.call(primaryPrompt);
        } catch (Exception e) {
            // 한도 초과 에러 발생하면 이쪽으로 넘어옴.
            System.out.println("1차 한도 초과... 원인: " + e.getMessage());
            // 모델 설정 변수 (Gemini-2.5-flash)
            GoogleGenAiChatOptions fallbackOptions = GoogleGenAiChatOptions.builder()
                    .model("gemini-2.5-flash").temperature(0.0).responseMimeType("application/json").build();

            try {
                // Gemini-2.5-Flash 주입
                Prompt fallbackPrompt = new Prompt(List.of(systemMessage, userMessage), fallbackOptions);
                response = chatModel.call(fallbackPrompt);
            } catch (Exception fallE) {
                // 아야...
                System.out.println("2차 예외 발생... 원인: " + fallE.getMessage());
                System.out.println("저런, 목업이나 뱉어야겠어요...");
                return getMockupReport();
            }
        }
        var result = response.getResult();
        var output = (result != null) ? result.getOutput() : null;
        String responseText = (output != null) ? output.getText() : null;

        if (responseText == null || responseText.isBlank()) {
            throw new RuntimeException("AI로부터 올바른 응답 데이터를 수신하지 못했습니다.");
        }

        try {
            AiResponseDto responseDto = objectMapper.readerFor(AiResponseDto.class)
                    .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(responseText.trim());
            Map<String, Object> reportMap = objectMapper.convertValue(responseDto, Map.class);
            userService.saveAiReport(uid, reportMap);

            return responseDto;
        } catch (Exception e) {
            throw new RuntimeException("Gemini JSON 바인딩 오류 발생. 원인: " + e.getMessage(), e);
        }
    }

    // 분석 시간 제한
    private AiResponseDto validAiCooldown(Map<String, Object> userProfile, long mins) {
        if (userProfile == null || !userProfile.containsKey("lastAiAnalyzed")) {
            return null;
        }

        try {
            Object timestampObj = userProfile.get("lastAiAnalyzed");
            Instant lastAnalyzed = null;

            if (timestampObj instanceof com.google.cloud.Timestamp) {
                lastAnalyzed = ((com.google.cloud.Timestamp) timestampObj).toSqlTimestamp().toInstant();
            } else if (timestampObj instanceof Map) {
                Map<?, ?> tsMap = (Map<?, ?>) timestampObj;
                if (tsMap.containsKey("seconds")) {
                    long seconds = Long.parseLong(tsMap.get("seconds").toString());
                    lastAnalyzed = Instant.ofEpochSecond(seconds);
                }
            }

            if (lastAnalyzed == null) { return null; }

            Instant now = Instant.now();
            long secondsElapsed = Duration.between(lastAnalyzed, now).toSeconds();
            long cooldownSeconds = mins * 60;

            if (secondsElapsed < cooldownSeconds) {
                if (userProfile.containsKey("lastAiReport") && userProfile.get("lastAiReport") != null) {
                    Object lastAiReport = userProfile.get("lastAiReport");
                    System.out.println("AI 시간 제한내 요청, 기존 리포트를 반환");
                    return objectMapper.convertValue(lastAiReport, AiResponseDto.class);
                }
            }
        } catch (Exception e) {
            System.err.println("AI 제한 연산 중 예외: " + e.getMessage());
        }

        return null;
    }

    // 프롬프트 텍스트 가져오기
    public String getSystemPrompt() throws Exception {
        try (InputStream inp = getClass().getResourceAsStream("/system_prompt.txt")) {
            if (inp == null) {
                System.err.println("에러: 프롬프트 파일을 찾을 수 없습니다.");
                return "";
            }
            System.out.println("프롬프트 로드 완료!: " + inp);
            return new String(inp.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("에러: 입출력 오류 발생. 원인: " + e.getMessage());
            return "";
        }
    }

    // 한도를 초과한 우리에게 주는 목업 선물...
    public AiResponseDto getMockupReport() throws Exception {
        String mockJson = """
        {
          "recommendedTracks": {
            "primaryTrack": { "trackName": "1차 트랙(주)", "reason": "1차 트랙을 추천하는 이유가 나오는 곳입니다." },
            "secondaryTrack": { "trackName": "2차 트랙(부)", "reason": "2차 트랙을 추천하는 이유가 나오는 곳입니다." }
          },
          "nextSemesterPlan": {
            "recommendedCourses": [
              { "courseId": "48400777", "courseName": "교과목1", "reason": "추천하는 이유1" },
              { "courseId": "48400888", "courseName": "교과목2", "reason": "추천하는 이유2" },
              { "courseId": "48400999", "courseName": "교과목3", "reason": "추천하는 이유3" }
            ]
          },
          "trackLearningSolution": {
            "trackGuides": ["트랙 가이드"],
            "actionPlans": ["플랜"]
          }
        }
        """;
        return objectMapper.readValue(mockJson, AiResponseDto.class);
    }
}
