package com.chatapp.controller;

import com.chatapp.service.UserService;
import com.chatapp.tracker.ActiveChatTracker;
import com.chatapp.dto.MessageDto;
import com.chatapp.service.MessageService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@AllArgsConstructor
@Controller
public class MessageWebSocketController {

        private final MessageService messageService;
        private final SimpMessagingTemplate messagingTemplate;
        private final ActiveChatTracker activeChatTracker;
        private final UserService userService;


        @MessageMapping("/send")
        public void sendMessage(MessageDto messageDto, Principal principal, SimpMessageHeaderAccessor accessor){

                // Fallback to the STOMP accessor if Principal is unavailable.
                Principal effectivePrincipal = principal;
                if (effectivePrincipal == null && accessor != null) {
                    effectivePrincipal = accessor.getUser();
                }

                if (effectivePrincipal == null) {
                        log.warn("Rejected unauthenticated STOMP message");
                    throw new MessagingException("Unauthenticated STOMP message");
                }

                log.debug("Authenticated STOMP message from user '{}'", effectivePrincipal.getName());

                // Derive the sender from the authenticated Principal.
                String username = effectivePrincipal.getName();
                Long senderId = userService.getUserIdByUserName(username);

                // Ignore any senderId supplied by the client.
                messageDto.setSenderId(senderId);

                MessageDto responseDto = messageService.sendMessage(messageDto);

                if (messageDto.getReceiverId() == null) {
                        messagingTemplate.convertAndSend("/topic/messages", responseDto);
                } else {
                        messagingTemplate.convertAndSend("/topic/user/" + messageDto.getReceiverId(), responseDto);
                        messagingTemplate.convertAndSend("/topic/user/" + messageDto.getSenderId(), responseDto);
                }
        }

        @MessageMapping("/leaveChat")
        public void leaveChat(Principal principal, SimpMessageHeaderAccessor accessor){

                Principal effectivePrincipal = principal;
                if (effectivePrincipal == null && accessor != null) {
                    effectivePrincipal = accessor.getUser();
                }

                if (effectivePrincipal == null) {
                        log.warn("Rejected unauthenticated STOMP message");
                    throw new MessagingException("Unauthenticated STOMP message");
                }

                String username = effectivePrincipal.getName();
                Long userId = userService.getUserIdByUserName(username);

                log.debug("Authenticated STOMP request from user '{}'", username);

                activeChatTracker.getActiveChats().remove(userId);

        }
}
