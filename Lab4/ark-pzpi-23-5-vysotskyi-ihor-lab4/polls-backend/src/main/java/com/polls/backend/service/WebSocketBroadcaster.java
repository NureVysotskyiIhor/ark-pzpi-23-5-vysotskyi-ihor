package com.polls.backend.service;

import com.polls.backend.handler.PollWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class WebSocketBroadcaster {

    @Autowired
    private PollWebSocketHandler webSocketHandler;

    /**
     * Трансляція оновлених результатів опитування
     */
    public void broadcastPollResults(UUID pollId, Object statistics) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "poll_results_update");
        data.put("topic", "polls/" + pollId + "/results");
        data.put("data", statistics);
        data.put("timestamp", System.currentTimeMillis());

        webSocketHandler.broadcast("polls/" + pollId + "/results", data);
    }

    /**
     * Трансляція нового опитування
     */
    public void broadcastNewPoll(Object poll) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "new_poll");
        data.put("topic", "polls/new");
        data.put("data", poll);
        data.put("timestamp", System.currentTimeMillis());

        webSocketHandler.broadcast("polls/new", data);
    }

    /**
     * Трансляція закриття опитування
     */
    public void broadcastPollClosed(UUID pollId, String title) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "poll_closed");
        data.put("topic", "polls/" + pollId + "/status");
        data.put("pollId", pollId);
        data.put("title", title);
        data.put("message", "Голосування закрито");
        data.put("timestamp", System.currentTimeMillis());

        webSocketHandler.broadcast("polls/" + pollId + "/status", data);
    }

    /**
     * Трансляція активних користувачів
     */
    public void broadcastActiveUsers(UUID pollId, int count) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "active_users_update");
        data.put("topic", "polls/" + pollId + "/users");
        data.put("pollId", pollId);
        data.put("activeUsers", count);
        data.put("timestamp", System.currentTimeMillis());

        webSocketHandler.broadcast("polls/" + pollId + "/users", data);
    }
}