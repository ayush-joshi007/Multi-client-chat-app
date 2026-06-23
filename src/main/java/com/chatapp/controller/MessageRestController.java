package com.chatapp.controller;

import com.chatapp.dto.MessageDto;
import com.chatapp.service.MessageService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@AllArgsConstructor
@RestController
public class MessageRestController {

    private MessageService messageService;

    @GetMapping("/messages")
    public List<MessageDto> getMessages(){
        return messageService.getHistory();
    }

    @GetMapping("/messages/private")
    public List<MessageDto> getPrivateMessages(
            @RequestParam Long senderId,
            @RequestParam Long receiverId){

        return messageService.getPrivateHistory(senderId, receiverId);
    }
}
