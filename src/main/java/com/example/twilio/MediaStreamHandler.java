package com.example.twilio;

import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class MediaStreamHandler extends TextWebSocketHandler {

    @Autowired
    private SpeechSettings speechSettings;

    private final ConcurrentLinkedQueue<byte[]> audioBuffer = new ConcurrentLinkedQueue<>();
    // 버퍼 크기를 16000(2초)로 증가
    private static final int CHUNK_SIZE = 16000;
    private int processedChunks = 0;
    private int totalBytesProcessed = 0;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("WebSocket 연결이 설정되었습니다: " + session.getId());
        audioBuffer.clear();
        processedChunks = 0;
        totalBytesProcessed = 0;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JSONObject jsonMessage = new JSONObject(message.getPayload());
            String event = jsonMessage.getString("event");

            switch (event) {
                case "connected":
                    handleConnected(jsonMessage);
                    break;
                case "start":
                    handleStart(jsonMessage);
                    break;
                case "media":
                    handleMedia(jsonMessage);
                    break;
                case "stop":
                    handleStop(jsonMessage);
                    break;
                default:
                    System.out.println("알 수 없는 이벤트: " + event);
            }
        } catch (Exception e) {
            System.err.println("메시지 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleConnected(JSONObject message) {
        System.out.println("Media Stream이 연결되었습니다");
        System.out.println("프로토콜: " + message.optString("protocol", "unknown"));
        System.out.println("스트림 SID: " + message.optString("streamSid", "unknown"));
    }

    private void handleStart(JSONObject message) {
        System.out.println("스트리밍이 시작되었습니다");
        JSONObject startInfo = message.optJSONObject("start");
        if (startInfo != null) {
            System.out.println("미디어 형식: " + startInfo.toString());
            System.out.println("트랙 정보:");
            System.out.println("- 채널: " + startInfo.optInt("channels", 1));
            System.out.println("- 샘플레이트: " + startInfo.optInt("rate", 8000));
            System.out.println("- 페이로드 타입: " + startInfo.optString("payload", "unknown"));
        }
    }

    protected void handleMedia(JSONObject message) {
        try {
            JSONObject media = message.getJSONObject("media");
            String payload = media.getString("payload");
            byte[] audioChunk = Base64.getDecoder().decode(payload);

            audioBuffer.offer(audioChunk);
            totalBytesProcessed += audioChunk.length;

            if (getTotalBufferSize() >= CHUNK_SIZE) {
                processAudioBuffer();
                processedChunks++;
//                System.out.println("청크 처리 #" + processedChunks + ", 총 처리된 바이트: " + totalBytesProcessed);
            }

        } catch (Exception e) {
            System.err.println("미디어 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void processAudioBuffer() {
        try {
            byte[] combinedAudio = combineAudioChunks();
            byte[] convertedAudio = convertMulawToLinear16(combinedAudio);

            // 변환된 오디오를 파일로 저장 (디버깅용)
            saveConvertedAudio(convertedAudio);

            try (SpeechClient speechClient = SpeechClient.create(speechSettings)) {
                RecognitionConfig config = RecognitionConfig.newBuilder()
                        .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                        .setSampleRateHertz(8000)
                        .setLanguageCode("ko-KR")
                        .setUseEnhanced(true)
                        .setEnableAutomaticPunctuation(true)
                        .build();

                RecognitionAudio audio = RecognitionAudio.newBuilder()
                        .setContent(ByteString.copyFrom(convertedAudio))
                        .build();

                long startTime = System.currentTimeMillis();
//                System.out.println("음성 인식 요청 시작 - 데이터 크기: " + convertedAudio.length + " bytes");

                RecognizeResponse response = speechClient.recognize(config, audio);
                List<SpeechRecognitionResult> results = response.getResultsList();

                long processTime = System.currentTimeMillis() - startTime;
//                System.out.println("음성 인식 처리 시간: " + processTime + "ms");

                if (!results.isEmpty()) {
                    for (SpeechRecognitionResult result : results) {
                        for (SpeechRecognitionAlternative alternative : result.getAlternativesList()) {
                            float confidence = alternative.getConfidence();
                            String transcript = alternative.getTranscript();


                            System.out.println("=== 음성 인식 결과 ===");
                            System.out.println("텍스트: " + transcript);
                            System.out.println("신뢰도: " + String.format("%.2f", confidence));
                            System.out.println("=====================");

                        }
                    }
                } else {
                    System.out.println("인식된 음성 없음");
                }
            }

            audioBuffer.clear();

        } catch (Exception e) {
            System.err.println("오디오 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int getTotalBufferSize() {
        return audioBuffer.stream().mapToInt(chunk -> chunk.length).sum();
    }

    private byte[] combineAudioChunks() {
        int totalSize = getTotalBufferSize();
        byte[] combined = new byte[totalSize];
        int offset = 0;

        while (!audioBuffer.isEmpty()) {
            byte[] chunk = audioBuffer.poll();
            System.arraycopy(chunk, 0, combined, offset, chunk.length);
            offset += chunk.length;
        }

        return combined;
    }

    private byte[] convertMulawToLinear16(byte[] mulawData) {
        short[] MULAW_DECODE_TABLE = new short[256];
        for (int i = 0; i < 256; i++) {
            int mu = ~i;
            int sign = (mu & 0x80) >> 7;
            int exponent = (mu & 0x70) >> 4;
            int mantissa = mu & 0x0f;
            int magnitude = ((mantissa << 3) + 0x84) << exponent;
            magnitude = magnitude >> 3;
            MULAW_DECODE_TABLE[i] = (short) (sign == 1 ? -magnitude : magnitude);
        }

        byte[] linearData = new byte[mulawData.length * 2];
        for (int i = 0; i < mulawData.length; i++) {
            short decodedSample = MULAW_DECODE_TABLE[mulawData[i] & 0xFF];
            linearData[i * 2] = (byte) (decodedSample & 0xFF);
            linearData[i * 2 + 1] = (byte) ((decodedSample >> 8) & 0xFF);
        }

        return linearData;
    }

    private void saveConvertedAudio(byte[] audioData) throws IOException {
        try (FileOutputStream fos = new FileOutputStream("converted_audio.wav")) {
            fos.write(audioData);
        }
//        System.out.println("Converted audio saved successfully.");
    }

    private void handleStop(JSONObject message) {
        System.out.println("스트리밍이 종료되었습니다");
        System.out.println("최종 처리 통계:");
        System.out.println("- 총 처리된 청크 수: " + processedChunks);
        System.out.println("- 총 처리된 바이트: " + totalBytesProcessed);

        if (!audioBuffer.isEmpty()) {
            processAudioBuffer();
        }
    }
}