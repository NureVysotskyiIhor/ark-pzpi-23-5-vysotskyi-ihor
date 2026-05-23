package com.polls.backend.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.polls.backend.service.PollService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PollWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(PollWebSocketHandler.class);
    private final ObjectMapper objectMapper;

    public PollWebSocketHandler() {
        this.objectMapper = new ObjectMapper();
        // ⭐ Регистрируем модуль для работы с Java 8 датами
        this.objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    }

    private final Map<String, Set<WebSocketSession>> pollSubscriptions = new ConcurrentHashMap<>();

    @Autowired
    private PollService pollService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("WebSocket client connected. Session: {}", session.getId());
        Map<String, Object> welcome = new HashMap<>();
        welcome.put("type", "connection_established");
        welcome.put("message", "Ви підключені до WebSocket сервера");
        welcome.put("sessionId", session.getId());
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(welcome)));
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            logger.debug("Received message: {}", payload);

            Map<String, Object> request = objectMapper.readValue(payload, Map.class);
            String action = (String) request.get("action");
            String topic = (String) request.get("topic");

            if ("subscribe".equals(action)) {
                handleSubscribe(session, topic);
            } else if ("unsubscribe".equals(action)) {
                handleUnsubscribe(session, topic);
            } else if ("ping".equals(action)) {
                handlePing(session);
            } else {
                sendError(session, "Невідомий action: " + action);
            }
        } catch (Exception e) {
            logger.error("Error processing message", e);
            sendError(session, "Помилка: " + e.getMessage());
        }
    }

    private void handleSubscribe(WebSocketSession session, String topic) throws IOException {
        logger.info("Subscribe to topic: {} (Session: {})", topic, session.getId());
        pollSubscriptions.computeIfAbsent(topic, k -> ConcurrentHashMap.newKeySet()).add(session);

        Map<String, Object> response = new HashMap<>();
        response.put("type", "subscription_confirmed");
        response.put("topic", topic);
        response.put("message", "Ви підписані на: " + topic);
        response.put("activeSubscribers", pollSubscriptions.get(topic).size());
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    private void handleUnsubscribe(WebSocketSession session, String topic) throws IOException {
        logger.info("Unsubscribe from topic: {} (Session: {})", topic, session.getId());
        Set<WebSocketSession> subscribers = pollSubscriptions.get(topic);
        if (subscribers != null) {
            subscribers.remove(session);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("type", "unsubscription_confirmed");
        response.put("topic", topic);
        response.put("message", "Ви відписані від: " + topic);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    private void handlePing(WebSocketSession session) throws IOException {
        Map<String, Object> pong = new HashMap<>();
        pong.put("type", "pong");
        pong.put("timestamp", System.currentTimeMillis());
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(pong)));
    }

    public void broadcast(String topic, Map<String, Object> data) {
        logger.debug("broadcast() called for topic: {}", topic);
        logger.debug("Active subscription keys: {}", pollSubscriptions.keySet());

        Set<WebSocketSession> subscribers = pollSubscriptions.get(topic);

        if (subscribers == null || subscribers.isEmpty()) {
            logger.warn("No subscribers for topic: {}", topic);
            return;
        }

        logger.debug("Found {} subscribers for topic: {}", subscribers.size(), topic);

        try {
            String message = objectMapper.writeValueAsString(data);

            for (WebSocketSession session : new HashSet<>(subscribers)) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                    logger.info("Message sent for topic: {}, sessionId: {}", topic, session.getId());
                } else {
                    logger.warn("Removing closed session: {}", session.getId());
                    subscribers.remove(session);
                }
            }
        } catch (Exception e) {
            logger.error("Broadcast failed for topic: {}", topic, e);
        }
    }

    // ⭐ СПЕЦИАЛИЗИРОВАННЫЙ МЕТОД - для результатів голосування
    public void broadcastPollResults(String pollId, Map<String, Object> stats) {
        String topic = "polls/" + pollId + "/results";

        Map<String, Object> update = new HashMap<>();
        update.put("type", "poll_results_update");
        update.put("topic", topic);
        update.put("data", stats);
        update.put("timestamp", System.currentTimeMillis());

        broadcast(topic, update);
    }

    private void sendError(WebSocketSession session, String message) throws IOException {
        Map<String, Object> error = new HashMap<>();
        error.put("type", "error");
        error.put("message", message);
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        logger.info("WebSocket client disconnected. Session: {}", session.getId());
        pollSubscriptions.values().forEach(set -> set.remove(session));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("WebSocket error for session {}: {}", session.getId(), exception.getMessage());
    }
}