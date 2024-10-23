package com.example.twilio;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.Say;
import com.twilio.type.PhoneNumber;
import com.twilio.twiml.voice.Connect;
import com.twilio.twiml.voice.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class VoiceController {
    @Value("${twilio.account_sid}")
    private String accountSid;

    @Value("${twilio.auth_token}")
    private String authToken;

    @Value("${twilio.phone_from}")
    private String fromPhone;

    @Value("${twilio.phone_to}")
    private String toPhone;

    @PostMapping("/make-call")
    public String makeCall() {
        Twilio.init(accountSid, authToken);

        String webhookUrl = "https://44a1-211-244-225-164.ngrok-free.app/voice";

        // 최신 Twilio API를 사용하여 통화 생성
        Call call = Call.creator(
                new PhoneNumber(toPhone),
                new PhoneNumber(fromPhone),
                URI.create(webhookUrl)
        ).create();

        return call.getSid();
    }

    @PostMapping(value = "/voice", produces = MediaType.APPLICATION_XML_VALUE)
    public String handleVoice() {
        // Stream URL 설정
        Stream stream = new Stream.Builder()
                .url("wss://44a1-211-244-225-164.ngrok-free.app/stream")
                .build();


        // Connect 요소에 Stream 추가
        Connect connect = new Connect.Builder()
                .stream(stream)
                .build();

        // TwiML 응답 생성
        VoiceResponse response = new VoiceResponse.Builder()
                .say(new Say.Builder("안녕하세요, 실시간 통화 테스트입니다.")
                        .language(Say.Language.KO_KR)
                        .build())
                .connect(connect)
                .build();

        return response.toXml();
    }
}