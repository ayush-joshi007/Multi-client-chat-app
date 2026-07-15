package com.chatapp.controller;

import com.chatapp.dto.ConversationDto;
import com.chatapp.service.MessageService;
import com.chatapp.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@AllArgsConstructor
@RestController
public class ConversationController {

    private final MessageService messageService;
    private final UserService userService;

    @GetMapping("/conversations")
    public List<ConversationDto> getConversations(Principal principal) {

        Long currentUserId = userService.getUserIdByUsername(principal.getName());

        return messageService.getConversationSummaries(currentUserId);
    }
}
