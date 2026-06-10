package com.chatapp.controller;

import com.chatapp.dto.MessageDto;
import com.chatapp.service.MessageService;
import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@AllArgsConstructor
@Controller
public class MessageWebSocketController {

        private final MessageService messageService;

        @MessageMapping("/send")
        @SendTo("/topic/messages")
        public MessageDto sendMessage(MessageDto messageDto) {
                return messageService.sendMessage(messageDto);
        }

}
