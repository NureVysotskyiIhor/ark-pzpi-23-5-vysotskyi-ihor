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
        logger.info("✅ WebSocket клієнт підключився. Session: {}", session.getId());
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
            logger.debug("📨 Отримано повідомлення: {}", payload);

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
            logger.error("❌ Помилка при обробці повідомлення", e);
            sendError(session, "Помилка: " + e.getMessage());
        }
    }

    private void handleSubscribe(WebSocketSession session, String topic) throws IOException {
        logger.info("🔔 Підписка на тему: {} (Session: {})", topic, session.getId());
        pollSubscriptions.computeIfAbsent(topic, k -> ConcurrentHashMap.newKeySet()).add(session);

        Map<String, Object> response = new HashMap<>();
        response.put("type", "subscription_confirmed");
        response.put("topic", topic);
        response.put("message", "Ви підписані на: " + topic);
        response.put("activeSubscribers", pollSubscriptions.get(topic).size());
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    private void handleUnsubscribe(WebSocketSession session, String topic) throws IOException {
        logger.info("🔕 Відписка від теми: {} (Session: {})", topic, session.getId());
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

    // ⭐ УНИВЕРСАЛЬНЫЙ МЕТОД - для любой трансляции
    public void broadcast(String topic, Map<String, Object> data) {
        System.out.println("🔥 broadcast() called for topic: " + topic);
        System.out.println("🔥 All subscriptions keys: " + pollSubscriptions.keySet());

        Set<WebSocketSession> subscribers = pollSubscriptions.get(topic);

        if (subscribers == null || subscribers.isEmpty()) {
            System.out.println("⚠️ Немає підписаних на тему: " + topic);
            logger.warn("⚠️ Немає підписаних на тему: {}", topic);
            return;
        }

        System.out.println("✅ Found " + subscribers.size() + " subscribers for topic: " + topic);

        try {
            String message = objectMapper.writeValueAsString(data);

            for (WebSocketSession session : new HashSet<>(subscribers)) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                    System.out.println("📤 Відправлено " + topic + " для session: " + session.getId());
                    logger.info("📤 Відправлено {}: {}", topic, session.getId());
                } else {
                    System.out.println("⚠️ Session closed: " + session.getId());
                    subscribers.remove(session);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Помилка при трансляції на " + topic + ": " + e.getMessage());
            e.printStackTrace();
            logger.error("❌ Помилка при трансляції на {}: {}", topic, e.getMessage());
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
        logger.info("❌ WebSocket клієнт відключився. Session: {}", session.getId());
        pollSubscriptions.values().forEach(set -> set.remove(session));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("⚠️ WebSocket помилка для {}: {}", session.getId(), exception.getMessage());
    }
}