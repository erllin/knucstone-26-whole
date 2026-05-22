package com.example.spring.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.example.spring.dto.TrackDto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Collections;

/**
 *  정규트랙들을 가져오는 부분입니다.
 *  CourseService 처럼 서버 가동시, DB에서 트랙들을 캐싱해옵니다.
 *
 */

@Service
public class TrackService {
    private final Firestore firestore;

    // 기존 UserService에서 분리했습니다.
    // 이쪽도 캐싱입니다. 전반적인 부분은 CourseService와 비슷합니다.
    private List<TrackDto> trackListCache = Collections.emptyList();

    public TrackService(Firestore firestore) {
        this.firestore = firestore;
    }

    // 서버가 가동되면, 트랙 리스트를 불러옵니다.
    @EventListener(ApplicationReadyEvent.class)
    public void initCourseCache() {
        synchronized (this) {
            try {
                System.out.println("Firestore에서 정규트랙 리스트를 불러오는 중입니다.");
                ApiFuture<QuerySnapshot> future = firestore.collection("trackList").get();
                QuerySnapshot querySnapshot = future.get();

                List<TrackDto> tempList = new ArrayList<>();

                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    if (doc.exists()) {
                        TrackDto track = doc.toObject(TrackDto.class);
                        // trackNo로 저장되어서 아래와 같이 두었습니다.
                        tempList.add(track);
                    }
                }
                // 순서대로 정렬합니다. (기존 getTracks의 tracks.sort())
                trackListCache.sort(Comparator.comparingInt(TrackDto::getTrackNo));
                // 불변!
                trackListCache = Collections.unmodifiableList(tempList);

                System.out.println("로드한 트랙 수: " + trackListCache.size());
            } catch (Exception e) {
                System.err.println("트랙 로드 오류 발생: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public List<TrackDto> getTracks() {
        return trackListCache;
    }
}
