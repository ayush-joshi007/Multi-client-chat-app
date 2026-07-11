package com.chatapp.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PresenceService {

    private final Map<Long, Set<String>> userIdToSessionIds;
    private final Map<String, Long> sessionIdToUserId;

    public PresenceService() {
        userIdToSessionIds = new ConcurrentHashMap<>();
        sessionIdToUserId = new ConcurrentHashMap<>();
    }

    public void registerSession(Long userId, String sessionId){
        userIdToSessionIds.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        sessionIdToUserId.put(sessionId, userId);
    }

    public Long removeSession(String sessionId) {

        Long userId = sessionIdToUserId.get(sessionId);
        if (userId == null) {
            return null;
        }

        Set<String> sessions = userIdToSessionIds.get(userId);
        if (sessions == null) {
            sessionIdToUserId.remove(sessionId);
            return userId;
        }

        sessions.remove(sessionId);
        sessionIdToUserId.remove(sessionId);

        if (sessions.isEmpty()) {
            userIdToSessionIds.remove(userId);
        }
        return userId;
    }

    public boolean isOnline(Long userId) {

        if (userId == null) {
            return false;
        }

        Set<String> sessions = userIdToSessionIds.get(userId);

        return sessions != null && !sessions.isEmpty();
    }
}
