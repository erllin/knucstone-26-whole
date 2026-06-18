package com.example.spring.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.example.spring.dto.CourseDto;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  강의 탐색 관련 로직입니다.
 *  UserProvider에도 일부 기능이 정의되어 있지만,
 *  Class의 CoursePopup 컴포넌트에 직접적으로 쓰이는 부분입니다.
 *  사용자가 강의번호를 입력하고, 엔터를 치거나 입력창을 벗어나면 자동으로 강의번호를 검색해 정보를 가져옵니다.
 *
 *  처음 서버를 가동할 때, 전체 강의 데이터를 메모리로 로드(캐싱)하도록 변경했습니다.
 *  감사합니다.
 *
 *  initCourseCache: 서버가 가동되면, DB로부터 전체 강의 데이터를 서버 메모리(캐시)로 가져옵니다. (읽기 약 75회정도.)
 *
 *  getAllCourses: 캐시에서 전체 강의 데이터를 가져옵니다.
 *                  Ai 프롬프트에 쓰일 수 있을 것 같아서, 남겨두었습니다.
 *  getCourseById:  강의번호(courseId)를 이용해 특정 강의 하나를 잡는 기능입니다.
 *                  이 부분이, CoursePopup의 검색 부분에 적용됩니다.
 */


// "CourseList" 데이터 조회 서비스
@Service
public class CourseService {
    // 초기화한 값을 끝까지 유지 (FirebaseConfig에서 만든 Firestore Bean)
    private final Firestore firestore;

    // 강의 정보를 ID 맵 & 캐싱.. (서버의 메모리가 꽤 남아서 괜찮을 것 같아요.)
    // 혹시 몰라서, 전공 전체 리스트도 가져와봐요. (AI 처리나 그런 부분에서 쓸 수 있을 것 같아서요.)
    private volatile Map<String, CourseDto> majorCourseMapCache = new ConcurrentHashMap<>();
    private List<CourseDto> majorCoursesListCache = Collections.emptyList();
    // 학교에서 추츨한 강의들을 담는 캐시
    private volatile Map<String, CourseDto> generalCourseMapCache = new ConcurrentHashMap<>();

    // Firestore 값을 주입함.
    public CourseService(Firestore firestore) {
        this.firestore = firestore;
    }

    // 서버가 가동되면, 데이터를 로드해오는 부분입니다.
    @EventListener(ApplicationReadyEvent.class)
    public void initCourseCache() {
        synchronized (this) {
            try {
                System.out.println("Firestore에서 강의 리스트를 불러오는 중입니다.");

                ApiFuture<QuerySnapshot> future = firestore.collection("courseList").get();
                ApiFuture<QuerySnapshot> future2 = firestore.collection("courseListGeneral").get();

                QuerySnapshot querySnapshot = future.get();
                QuerySnapshot querySnapshot2 = future2.get();

                Map<String, CourseDto> tempMap =  new ConcurrentHashMap<>();
                List<CourseDto> tempList = new ArrayList<>();

                // 전공 과목에 대해.
                for (QueryDocumentSnapshot doc : querySnapshot.getDocuments()) {
                    CourseDto courseDto = doc.toObject(CourseDto.class);
                    courseDto.setId(doc.getId());

                    tempMap.put(doc.getId(), courseDto);
                    tempList.add(courseDto);
                }

                // 캐시를 새로운 tempMap으로 교체
                majorCourseMapCache.clear();
                majorCourseMapCache.putAll(tempMap);
                // 불변 리스트로 두는 것이 좋다고 하네요.
                majorCoursesListCache = Collections.unmodifiableList(tempList);

                tempMap.clear();

                for (QueryDocumentSnapshot doc : querySnapshot2.getDocuments()) {
                    CourseDto courseDto = doc.toObject(CourseDto.class);
                    courseDto.setId(doc.getId());

                    tempMap.put(doc.getId(), courseDto);
                }

                generalCourseMapCache.clear();
                generalCourseMapCache.putAll(tempMap);

                System.out.println("로드한 학과 강의 수: " + majorCourseMapCache.size());
                System.out.println("로드한 학교 강의 수: " + generalCourseMapCache.size());
            } catch (Exception e) {
                System.err.println("강의 로드 오류 발생: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // 모든 전공 Course 가져오기
    public List<CourseDto> getMajorCourses()  {
        return majorCoursesListCache;
    }

    // 낱개 ID 탐색
    public CourseDto getCourseById(String id) {
        CourseDto courseDto = majorCourseMapCache.get(id);
        if(courseDto != null) {
            return courseDto;
        } else {
            return generalCourseMapCache.get(id);
        }
    }

    public CourseDto getCourseByIdOrName(String query) {
        if (query == null || query.trim().isEmpty()) { return null; }
        String trimQuery = query.trim();

        CourseDto courseDto = getCourseById(trimQuery);
        if (courseDto != null) {
            return courseDto;
        }

        Optional<CourseDto> foundInMajor = majorCourseMapCache.values().stream()
                .filter(c -> c.getName() != null && c.getName().trim().equals(trimQuery))
                .findFirst();
        if (foundInMajor.isPresent()) { return foundInMajor.get(); }

        Optional<CourseDto> foundInGeneral = generalCourseMapCache.values().stream()
                .filter(c -> c.getName() != null && c.getName().trim().equals(trimQuery))
                .findFirst();

        return foundInGeneral.orElse(null);
    }
}
