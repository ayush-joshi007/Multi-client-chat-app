package com.chatapp.controller;

import com.chatapp.dto.MessageDto;
import com.chatapp.projection.UnreadCountProjection;
import com.chatapp.service.MessageService;
import com.chatapp.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@AllArgsConstructor
@RestController
public class MessageRestController {

    private MessageService messageService;
    private UserService userService;


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

    @GetMapping("/messages/unread-counts")
    public List<UnreadCountProjection> getUnreadCounts(Principal principal) {

        String username = principal.getName();

        Long receiverId = userService.getUserIdByUsername(username);

        return messageService.getUnreadCounts(receiverId);
    }
}
