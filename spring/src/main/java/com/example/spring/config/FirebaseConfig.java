package com.example.spring.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    private void initializeFirebaseApp() {
        if (FirebaseApp.getApps().isEmpty()) {
            try {
                // .json으로부터 SDK 설정값 불러오기 시도.
                InputStream serviceAccount = getClass().getClassLoader()
                        .getResourceAsStream("firebase-service-account.json");

                if (serviceAccount == null) {
                    throw new IllegalStateException("resources 폴더에서 firebase-service-account.json 파일을 찾을 수 없습니다.");
                }

                // options = 서비스 인증 계정 정보 설정
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                System.out.println("Firebase 연동 준비 완료!");

            } catch (IOException e) {
                System.err.println("Firebase 서비스 계정 로드 실패: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }

    //
    @Bean
    public Firestore firestore() {
        initializeFirebaseApp();
        return FirestoreClient.getFirestore();
    }

    // Firebase Auth 인증 인스턴스
    @Bean
    public FirebaseAuth firebaseAuth() {
        initializeFirebaseApp();
        return FirebaseAuth.getInstance();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
