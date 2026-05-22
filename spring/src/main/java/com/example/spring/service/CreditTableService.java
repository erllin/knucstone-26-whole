package com.example.spring.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  이수학점 표를 가져오는 부분입니다.
 *  CourseService 처럼 서버 가동시, DB에서 이수학점 표들을 캐싱해옵니다.
 *  캐싱된 데이터를 기반으로, 사용자의 학사정보에 맞게 한 개만 남긴(matchedDetail) 데이터를 react쪽으로 넘깁니다.
 *
 *  DB에서 저장된 구조 때문에, 분리하는 방법이 일부 이상할 수 있어서.. 이 점 참고해주세요.
 *  getUserMatchedTable: 학사정보랑 비교해서 이수학점표를 가공해 넘기는 메소드입니다.
 *                       아마, 이 또한 Ai 프롬프트에 사용될 수 있을 것 같습니다.
 */


@Service
public class CreditTableService {

    // 초기화된 Firestore Bean 가져오기
    private final Firestore firestore;

    // 이수학점 테이블도, 이후에 크게 변하지 않을 것으로 생각되어 캐싱으로 바꾸었습니다.
    private final Map<String, Map<String, Object>> creditTableCache = new ConcurrentHashMap<>();

    // Firestore Bean 주입.
    public CreditTableService(Firestore firestore) {
        this.firestore = firestore;
    }

    // 서버 실행되면, DB->메모리로 로드합니다.
    @EventListener(ApplicationReadyEvent.class)
    public void initCreditTableCache() {
        synchronized (this) {
            try {
                System.out.println("Firestore에서 이수학점표 묶음을 불러오는 중입니다.");
                ApiFuture<QuerySnapshot> future = firestore.collection("creditTables").get();
                QuerySnapshot querySnapshot = future.get();

                Map<String, Map<String, Object>> tempCache = new ConcurrentHashMap<>();
                for (QueryDocumentSnapshot doc : querySnapshot.getDocuments()) {
                    // 대부분은 문제가 발생하지 않겠지만, 비어있는 경우를 대비해 두는 것이 좋다고 하네요.
                    if (doc.exists() && doc.getData() != null) {
                        tempCache.put(doc.getId(), doc.getData());
                    }
                }

                creditTableCache.clear();
                creditTableCache.putAll(tempCache);
                System.out.println("로드한 이수학점표 수: " + creditTableCache.size());
            } catch (Exception e) {
                System.err.println("이수학점표 로드 오류 발생: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public Map<String, Object> getUserMatchedCreditTable(String uid, String documentId) throws Exception {

        // users 컬렉션에서 해당 유저의 학사 문서 로드
        DocumentSnapshot userDoc = firestore.collection("users").document(uid).get().get();
        if (!userDoc.exists()) {
            throw new RuntimeException("해당 유저의 정보를 조회할 수 없습니다. (uid: " + uid + ")");
        }
        // 유저의 데이터 필드 분리
        Map<String, Object> userData = userDoc.getData();
        if (userData == null) {
            throw new IllegalStateException("유저 데이터 필드가 비어 있습니다.");
        }
        // 이수학점표와 묶여야하는 필드들이 존재하는지?
        String userDept = (String) userData.get("department");      // 학과
        Object userMajorTypeObj = userData.get("majorType");        // 전공유형

        if (userDept == null || userMajorTypeObj == null) {
            throw new IllegalStateException("유저 문서에 department 또는 majorType 정보가 입력되지 않았습니다.");
        }

        int userMajorType = ((Number) userMajorTypeObj).intValue();

        // 위에서 미리 만든 캐시를 통해 이수학점표를 찾습니다. (구조 상, documentId로 접근하는 것이 맞습니다.)
        Map<String, Object> deptData = creditTableCache.get(documentId);
        if (deptData == null) {
            throw new RuntimeException("요청한 이수학점표가 캐시에 존재하지 않습니다." + documentId);
        }

        // 매칭하는 곳 입니다. (cs_2026만 사용하고 불러오기에, 여기서 문제가 발생하지는 않을 것 같습니다.)
        String firebaseDept = (String) deptData.get("department");
        if (firebaseDept == null || !firebaseDept.trim().equals(userDept.trim())) {
            throw new IllegalArgumentException("유저의 학과(" + userDept + ")와 이수기준표의 학과(" + firebaseDept + ")가 일치하지 않습니다.");
        }

        // 노란 줄 제거! (uncheckedType)
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> detailsList = (List<Map<String, Object>>) deptData.get("details");

        if (detailsList == null || detailsList.isEmpty()) {
            throw new RuntimeException("데이터 내부의 세부 이수 요건(details) 배열이 존재하지 않습니다.");
        }

        // 유저의 majorType과 완벽히 일치하는 이수학점만 가져옵니다.
        Map<String, Object> matchedDetail = detailsList.stream()
                .filter(detail -> {
                    Object typeObj = detail.get("majorType");
                    return typeObj != null && ((Number) typeObj).intValue() == userMajorType;
                })
                .findFirst()
                .orElseThrow(() -> new RuntimeException("학과 기준표에 해당 전공 구분 번호(majorType: " + userMajorType + ") 사양이 기술되어 있지 않습니다."));

        // 수신부 (tracks의 details를 모두 날리고, 필요한 matchedDetail만 붙여서 던집니다.)
        Map<String, Object> responseMap = new HashMap<>(deptData);
        responseMap.remove("details");
        responseMap.put("matchedDetail", matchedDetail);

        return responseMap;
    }
}


