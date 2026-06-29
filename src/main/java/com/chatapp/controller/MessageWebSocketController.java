package com.chatapp.controller;

import com.chatapp.tracker.ActiveChatTracker;
import com.chatapp.tracker.OnlineUserTracker;
import com.chatapp.dto.MessageDto;
import com.chatapp.service.MessageService;
import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@AllArgsConstructor
@Controller
public class MessageWebSocketController {

        private final MessageService messageService;
        private final SimpMessagingTemplate messagingTemplate;
        private final OnlineUserTracker onlineUserTracker;
        private final ActiveChatTracker activeChatTracker;


        @MessageMapping("/send")
        public void sendMessage(MessageDto messageDto){
                MessageDto responseDto = messageService.sendMessage(messageDto);

                if(messageDto.getReceiverId()==null){

                        messagingTemplate.convertAndSend("/topic/messages", responseDto);

                }
                else{

                        messagingTemplate.convertAndSend("/topic/user/" + messageDto.getReceiverId(),responseDto);

                        messagingTemplate.convertAndSend("/topic/user/" + messageDto.getSenderId(),responseDto);

                }
        }

        @MessageMapping("/online")
        public void markOnline(Long userId, SimpMessageHeaderAccessor accessor){

                String sessionId = accessor.getSessionId();

                onlineUserTracker.getOnlineUsers().add(userId);

                messageService.markPendingMessagesAsDelivered(userId);

                onlineUserTracker.getSessionToUser().put(sessionId, userId);

                messagingTemplate.convertAndSend("/topic/users", "refresh");

        }

        @MessageMapping("/leaveChat")
        public void leaveChat(Long userId){

                activeChatTracker.getActiveChats().remove(userId);

        }
}
