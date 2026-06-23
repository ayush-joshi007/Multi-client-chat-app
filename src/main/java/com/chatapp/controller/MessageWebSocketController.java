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
                MessageDto responseDto = messageService.sendMessage(messageDto);

                if(messageDto.getReceiverId()==null){

                        messagingTemplate.convertAndSend("/topic/messages", responseDto);

                }
                else{

                        messagingTemplate.convertAndSend("/topic/user/" + messageDto.getReceiverId(),responseDto);

                        messagingTemplate.convertAndSend("/topic/user/" + messageDto.getSenderId(),responseDto);
                }
        }

}
