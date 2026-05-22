package com.example.spring.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.example.spring.dto.UserCombinedDto;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 *  사용자 데이터(userProfile, userCourses, userSemester) 편집 관련의 대부분이 여기 몰려 있습니다.
 *  이 부분은 여러번 테스트 해보긴 해서 동작 상에는 문제가 없는 것 같지만,
 *  속도가 조금 느리게 처리되어서...
 *
 *  getUserCombinedData: 유저 데이터를 전체적으로 조회합니다. (users/{uid} 쪽 데이터를 모두 가져옵니다.)
 *                       userProfile, userCourses, userSemesters
 *  updateSemesterTaken: taken(연차?)를 새로 갱신합니다. 하위 강의에 대해서도 모두 taken값을 갱신해줍니다.
 *  deleteSemester: 학기를 삭제합니다. 하위 강의들을 모두 삭제합니다. (연쇄처리)
 *  updateProfile: 학사정보를 갱신합니다.
 *  addCourse: 하나의 강의를 추가합니다.
 *  deleteCourse: 하나의 강의를 삭제합니다. (중복 수강에서, 기존 수강했던 과목을 삭제하는 용도로 추로 씁니다.)
 *  addSemester: 학기를 새로 추가합니다.
 *  saveSemesterAndCourses: 학기정보, 수강이력 모두 일괄 업데이트 합니다.
 */

@Service
public class UserService {

    private final Firestore firestore;

    public UserService(Firestore firestore) {
        this.firestore = firestore;
    }

    // 1. 유저 데이터 조회
    public UserCombinedDto getUserCombinedData(String uid) throws Exception {
        UserCombinedDto dto = new UserCombinedDto();

        // 병렬 처리
        ApiFuture<DocumentSnapshot> profileFuture = firestore.collection("users").document(uid).get();
        ApiFuture<QuerySnapshot> coursesFuture = firestore.collection("users").document(uid).
                collection("userCourses").get();
        ApiFuture<QuerySnapshot> semestersFuture = firestore.collection("users").document(uid).
                collection("userSemesters").get();

        // 병렬 처리 대기
        DocumentSnapshot profileDoc = profileFuture.get();
        QuerySnapshot coursesSnap = coursesFuture.get();
        QuerySnapshot semestersSnap = semestersFuture.get();

        // userProfile (여기는 컬렉션 단위여서 Map 할 필요 없음.)
        dto.setUserProfile(profileDoc.exists() ? profileDoc.getData() : null);

        // userCourses (이 둘은 도큐먼트 안에 도큐먼트라서 따로 맵핑해놓아야함. )
        List<Map<String, Object>> courses = new ArrayList<>();
        for (QueryDocumentSnapshot document : coursesSnap.getDocuments()) {
            Map<String, Object> data = document.getData();
            if (data != null) {
                Map<String, Object> safeData = new HashMap<>(data); // 불변 Map 문제를 방지하기 위한 복사
                safeData.put("id", document.getId());
                courses.add(safeData);
            }
        }
        dto.setUserCourses(courses);

        // userSemesters
        List<Map<String, Object>> semesters = new ArrayList<>();
        for (QueryDocumentSnapshot document : semestersSnap.getDocuments()) {
            Map<String, Object> data = document.getData();
            if (data != null) {
                Map<String, Object> safeData = new HashMap<>(data);
                safeData.put("id", document.getId());
                semesters.add(safeData);
            }
        }
        dto.setUserSemesters(semesters);

        return dto;
    }

    // 학기 변경 및 하위 수강이력 일괄 업데이트 (네트워크 조회 분리)
    public void updateSemesterTaken(String uid, String semId, String takenOld, String takenNew) throws Exception {
        // 배치 생성 전에 변경 대상(이수이력)을 먼저 조회(GET)하여 대기 시간 최소화
        QuerySnapshot courseSnap = firestore.collection("users").document(uid)
                .collection("userCourses").whereEqualTo("taken", takenOld).get().get();

        // 배치 생성
        WriteBatch batch = firestore.batch();

        // 학기 정보 업데이트를 우선 배치에 등록
        DocumentReference semRef = firestore.collection("users").document(uid)
                .collection("userSemesters").document(semId);
        batch.update(semRef, "taken", takenNew);

        // 조건에 맞는 이수 이력쪽도 배치에 등록
        for (QueryDocumentSnapshot document : courseSnap.getDocuments()) {
            batch.update(document.getReference(), "taken", takenNew);
        }
        // 배치를 커밋 (POST?) = 업데이트 처리.
        batch.commit().get();
    }

    // 학기 삭제 및 하위 수강이력 연쇄 삭제
    public void deleteSemester(String uid, String semId, String taken) throws Exception {
        // 동일하게 대상을 먼저 GET.
        QuerySnapshot courseSnap = firestore.collection("users").document(uid)
                .collection("userCourses").whereEqualTo("taken", taken).get().get();

        WriteBatch batch = firestore.batch();

        // 학기 문서 삭제를 배치에 등록.
        DocumentReference semRef = firestore.collection("users").document(uid)
                .collection("userSemesters").document(semId);
        batch.delete(semRef);

        // 조건에 맞는 이수 이력쪽도 배치에 등록.
        for (QueryDocumentSnapshot document : courseSnap.getDocuments()) {
            batch.delete(document.getReference());
        }
        // 배치를 커밋해서 삭제 실행.
        batch.commit().get();
    }

    // 학사 정보를 갱신
    public void updateProfile(String uid, Map<String, Object> data) throws Exception {
        // 기존 속성들을 .merge(병합)하면서 SET 처리하는 방식임.
        firestore.collection("users").document(uid)
                .set(data, SetOptions.merge()).get();
    }

    // 강의 한 개 추가.
    public void addCourse(String uid, String cid, Map<String, Object> courseData) throws Exception {
        firestore.collection("users").document(uid)
                .collection("userCourses").document(cid)
                .set(courseData).get();
    }

    // 강의 한 개 삭제. => 해당 메커니즘은 재수강 과목 삭제 처리에서 사용되곤 함.
    public void deleteCourse(String uid, String cid) throws Exception {
        firestore.collection("users").document(uid)
                .collection("userCourses").document(cid)
                .delete().get();
    }

    // 학기 추가 (Map 데이터 오염 방지하기 위해 아래로 변경)
    public void addSemester(String uid, Map<String, Object> semesterData) throws Exception {
        DocumentReference docRef = firestore.collection("users").document(uid)
                .collection("userSemesters").document();

        // 파라미터로 넘어온 Map을 직접 수정하지 않고 복사본을 만들어 안전하게 저장 (아마 불변성 규칙 때문일 듯)
        Map<String, Object> dataToSave = new HashMap<>(semesterData);
        dataToSave.put("id", docRef.getId());

        docRef.set(dataToSave).get();
    }

    // 강의 팝업 저장 프로세스
    public void saveSemesterAndCourses(String uid, com.example.spring.dto.CourseSaveRequestDto req) throws Exception {
        // 삭제해야 할 기존 과목을 배치 생성 전에 먼저 GET
        List<DocumentReference> oldCourseRefs = new ArrayList<>();
        if (req.getOldTaken() != null && !req.getOldTaken().trim().isEmpty()) {
            QuerySnapshot oldCourseSnap = firestore.collection("users")
                    .document(uid).collection("userCourses")
                    .whereEqualTo("taken", req.getOldTaken()).get().get();
            for (QueryDocumentSnapshot doc : oldCourseSnap.getDocuments()) {
                oldCourseRefs.add(doc.getReference());
            }
        }

        // 배치 등록
        WriteBatch batch = firestore.batch();
        DocumentReference profileRef = firestore.collection("users").document(uid);
        String curSemId = req.getTargetSemId();

        // 학기 생성/수정 분기 처리
        if (curSemId == null || curSemId.trim().isEmpty()) {
            // 신규 학기 생성
            DocumentReference semRef = firestore.collection("users").document(uid).collection("userSemesters").document();
            curSemId = semRef.getId();

            Map<String, Object> semData = new HashMap<>();
            semData.put("id", curSemId);
            semData.put("isNonRegularTerm", req.getIsNonRegularTerm());
            semData.put("term", req.getTerm());
            semData.put("taken", req.getActiveTaken());
            semData.put("sortKey", req.getSortKey());
            batch.set(semRef, semData);

            // 학사정보 이수 학기 수 누적 업데이트
            boolean isRegular = (req.getIsNonRegularTerm() == 0);
            batch.update(profileRef, isRegular ? "regularTerm" : "nonRegularTerm", req.getTerm());
        } else {
            // 기존 학기 정보 갱신 (수정)
            DocumentReference semRef = firestore.collection("users").document(uid).collection("userSemesters").document(curSemId);
            Map<String, Object> updates = new HashMap<>();
            updates.put("taken", req.getActiveTaken());
            updates.put("sortKey", req.getSortKey());
            batch.update(semRef, updates);
        }

        // 교체 대상인 오래된 과목 일괄 삭제 예약
        for (DocumentReference ref : oldCourseRefs) {
            batch.delete(ref);
        }

        // 화면에서 넘어온 신규 과목 리스트 일괄 추가 예약
        if (req.getLocalCourses() != null) {
            for (Map<String, Object> courseMap : req.getLocalCourses()) {
                DocumentReference newCourseRef = firestore.collection("users")
                        .document(uid).collection("userCourses").document();

                // 얕은 복사 방지: 새로운 HashMap 객체를 생성하여 DTO 내부 데이터의 변형을 격리 (오염 방지)
                Map<String, Object> safeCourseData = new HashMap<>(courseMap);
                safeCourseData.put("id", newCourseRef.getId());
                safeCourseData.put("taken", req.getActiveTaken());

                batch.set(newCourseRef, safeCourseData);
            }
        }

        // 배치 커밋
        batch.commit().get();
    }
}