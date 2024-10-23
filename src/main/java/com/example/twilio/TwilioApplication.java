package com.example.twilio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@SpringBootApplication
@EnableWebSocket
public class TwilioApplication implements WebSocketConfigurer {

    public static void main(String[] args) {
        SpringApplication.run(TwilioApplication.class, args);
    }

    @Bean
    public MediaStreamHandler mediaStreamHandler() {
        return new MediaStreamHandler();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(mediaStreamHandler(), "/stream")
                .setAllowedOrigins("*");
    }
}