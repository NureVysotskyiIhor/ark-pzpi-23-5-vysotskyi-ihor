package com.polls.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import com.polls.backend.handler.PollWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketHandlerConfig implements WebSocketConfigurer {

    @Autowired
    private PollWebSocketHandler pollWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(pollWebSocketHandler, "/ws")
                .setAllowedOrigins("*");
    }
}