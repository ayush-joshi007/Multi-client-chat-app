package com.chatapp.controller;

import com.chatapp.dto.MessageDto;
import com.chatapp.service.MessageService;
import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@AllArgsConstructor
@Controller
public class MessageWebSocketController {

        private final MessageService messageService;
        private final SimpMessagingTemplate messagingTemplate;

        @MessageMapping("/send")
        public void sendMessage(MessageDto messageDto){
                String destination;
                if(messageDto.getReceiverId()==null) {
                        destination = "/topic/messages";
                }
                else{
                        destination = "/topic/user/" + messageDto.getReceiverId();
                }
                MessageDto responseDto = messageService.sendMessage(messageDto);
                messagingTemplate.convertAndSend(destination, responseDto);
        }

}
