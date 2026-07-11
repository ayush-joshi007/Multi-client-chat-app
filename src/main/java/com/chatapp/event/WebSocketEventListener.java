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
        Long userId = presenceService.removeSession(sessionId);

        if(userId == null){
            return;
        }

        if(!presenceService.isOnline(userId)){
            activeChatTracker.getActiveChats().remove(userId);
            messagingTemplate.convertAndSend("/topic/users", "refresh");
        }

    }

    @EventListener
    public void handleConnect(SessionConnectedEvent event){

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        Principal principal = accessor.getUser();

        if (principal == null) {
            principal = event.getUser();
        }

        if (sessionId == null || principal == null) {
            log.warn("Skipping presence registration. sessionId={}, principal={}", sessionId, principal);
            return;
        }

        String userName = principal.getName();
        Long userId = userService.getUserIdByUserName(userName);
        presenceService.registerSession(userId, sessionId);
        messageService.markPendingMessagesAsDelivered(userId);
        messagingTemplate.convertAndSend("/topic/users", "refresh");
    }
}
