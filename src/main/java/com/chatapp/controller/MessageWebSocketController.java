package com.chatapp.controller;

import com.chatapp.Mapper.impl.MessageMapper;
import com.chatapp.dto.MessageDto;
import com.chatapp.entity.MessageEntity;
import com.chatapp.repository.MessageRepository;
import com.chatapp.repository.UserRepository;
import com.chatapp.service.MessageService;
import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@AllArgsConstructor
@Controller
public class MessageWebSocketController {

        private final MessageService messageService;
        private final MessageMapper messageMapper;
        private final UserRepository userRepository;
        private final MessageRepository messageRepository;

        @MessageMapping("/send")
        @SendTo("/topic/messages")
        public MessageDto sendMessage(MessageDto messageDto){

                System.out.println("\n========================");
                System.out.println("SEND MESSAGE CALLED");
                System.out.println("DTO RECEIVED: " + messageDto);

                MessageEntity messageEntity = messageMapper.mapFrom(messageDto);

                System.out.println("AFTER MAPPING: " + messageEntity);
                System.out.println("ENTITY ID BEFORE SAVE: " + messageEntity.getId());

                if(messageDto.getSenderId() != null){

                        System.out.println("LOOKING FOR USER: " + messageDto.getSenderId());

                        userRepository.findById(messageDto.getSenderId())
                                .ifPresentOrElse(
                                        user -> {
                                                System.out.println("USER FOUND: " + user.getUserName());
                                                messageEntity.setSender(user);
                                        },
                                        () -> System.out.println("USER NOT FOUND!")
                                );
                }

                System.out.println("BEFORE SAVE:");
                System.out.println("ID = " + messageEntity.getId());
                System.out.println("CONTENT = " + messageEntity.getContent());
                System.out.println("SENDER = " + messageEntity.getSender());

                MessageEntity savedMessageEntity = messageRepository.save(messageEntity);

                System.out.println("AFTER SAVE:");
                System.out.println("ID = " + savedMessageEntity.getId());
                System.out.println("CONTENT = " + savedMessageEntity.getContent());
                System.out.println("SENDER = " + savedMessageEntity.getSender());

                MessageDto responseDto = messageMapper.mapTo(savedMessageEntity);

                if(savedMessageEntity.getSender() != null){
                        responseDto.setUserName(savedMessageEntity.getSender().getUserName());
                        responseDto.setSenderId(savedMessageEntity.getSender().getUserId());
                }

                System.out.println("RETURNING DTO: " + responseDto);
                System.out.println("========================\n");

                return responseDto;
        }

}
