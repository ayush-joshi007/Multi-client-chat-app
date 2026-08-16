package com.chatapp.event;

import com.chatapp.service.MessageService;
import com.chatapp.service.PresenceService;
import com.chatapp.service.UserService;
import com.chatapp.tracker.ActiveChatTracker;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Slf4j
@Component
@AllArgsConstructor
public class WebSocketEventListener {
    private final SimpMessagingTemplate messagingTemplate;
    private final ActiveChatTracker activeChatTracker;
    private final UserService userService;
    private final PresenceService presenceService;
    private final MessageService messageService;

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        log.info("[WS_DEBUG] WebSocket SessionDisconnectEvent - SessionId: {}", sessionId);

        Long userId = presenceService.removeSession(sessionId);
        log.debug("[WS_DEBUG] Removed session from presence service - UserId: {}", userId);

        if(userId == null){
            log.debug("[WS_DEBUG] No userId found for session {}, skipping presence update", sessionId);
            return;
        }

        if(!presenceService.isOnline(userId)){
            activeChatTracker.getActiveChats().remove(userId);
            messagingTemplate.convertAndSend("/topic/users", "refresh");
            log.info("[WS_DEBUG] User {} is now offline, sent refresh notification", userId);
        }

    }

    @EventListener
    public void handleConnect(SessionConnectedEvent event){
        log.info("[WS_DEBUG] WebSocket SessionConnectedEvent received");

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        Principal principal = accessor.getUser();

        if (principal == null) {
            principal = event.getUser();
        }

        if (sessionId == null || principal == null) {
            log.warn("[WS_DEBUG] Skipping presence registration - sessionId={}, principal={}", sessionId, principal);
            return;
        }

        String userName = principal.getName();
        log.info("[WS_DEBUG] WebSocket connection established - SessionId: {}, UserName: {}", sessionId, userName);
        
        Long userId = userService.getUserIdByUserName(userName);
        presenceService.registerSession(userId, sessionId);
        messageService.markPendingMessagesAsDelivered(userId);
        messagingTemplate.convertAndSend("/topic/users", "refresh");
        log.info("[WS_DEBUG] User {} registered as online via WebSocket session {}", userName, sessionId);
    }
}
