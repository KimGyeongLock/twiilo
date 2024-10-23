package com.example.twilio;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class GoogleSpeechToTextExample {
    public static void main(String[] args) {
        try {
            // 1. 파일 경로 확인
            String audioFilePath = "src/main/resources/output.wav";
            String credentialsPath = "src/main/resources/stt.json";

            Path audioPath = Paths.get(audioFilePath);
            Path credsPath = Paths.get(credentialsPath);

            // 2. 파일 존재 확인
            if (!Files.exists(audioPath)) {
                throw new IOException("오디오 파일을 찾을 수 없습니다: " + audioFilePath);
            }
            if (!Files.exists(credsPath)) {
                throw new IOException("인증 파일을 찾을 수 없습니다: " + credentialsPath);
            }

            // 3. Credentials 로드
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new FileInputStream(credentialsPath));

            // 4. Speech Client 생성
            try (SpeechClient speechClient = SpeechClient.create(
                    SpeechSettings.newBuilder()
                            .setCredentialsProvider(() -> credentials)
                            .build())) {

                // 5. 오디오 데이터 로드
                byte[] audioData = Files.readAllBytes(audioPath);
                ByteString audioBytes = ByteString.copyFrom(audioData);

                // 6. Recognition Config 설정
                RecognitionConfig config = RecognitionConfig.newBuilder()
                        .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                        .setSampleRateHertz(16000)
                        .setLanguageCode("ko-KR")
                        .setEnableAutomaticPunctuation(true)
                        .build();

                // 7. Recognition Audio 설정
                RecognitionAudio audio = RecognitionAudio.newBuilder()
                        .setContent(audioBytes)
                        .build();

                // 8. API 호출
                System.out.println("API 호출 시작...");
                RecognizeResponse response = speechClient.recognize(config, audio);
                List<SpeechRecognitionResult> results = response.getResultsList();

                // 9. 결과 처리
                if (results.isEmpty()) {
                    System.out.println("인식된 결과가 없습니다.");
                } else {
                    for (SpeechRecognitionResult result : results) {
                        System.out.println("변환 결과: " + result.getAlternativesList().get(0).getTranscript());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}