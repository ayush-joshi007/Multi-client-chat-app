package com.chatapp.event;

import com.chatapp.tracker.ActiveChatTracker;
import com.chatapp.tracker.OnlineUserTracker;
import lombok.AllArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@AllArgsConstructor
public class WebSocketEventListener {

    private final OnlineUserTracker onlineUserTracker;
    private final SimpMessagingTemplate messagingTemplate;
    private final ActiveChatTracker activeChatTracker;

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {

        String sessionId = event.getSessionId();
        Long userId = onlineUserTracker.getSessionToUser().get(sessionId);

        if(userId != null){
            onlineUserTracker.getOnlineUsers().remove(userId);

            activeChatTracker.getActiveChats().remove(userId);

            onlineUserTracker.getSessionToUser().remove(sessionId);

            messagingTemplate.convertAndSend("/topic/users", "refresh");
        }

    }
}
