package com.chatapp.controller;

import com.chatapp.dto.MessageDto;
import com.chatapp.projection.UnreadCountProjection;
import com.chatapp.service.MessageService;
import com.chatapp.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@AllArgsConstructor
@RestController
public class MessageRestController {

    private final MessageService messageService;
    private final UserService userService;

    @GetMapping("/messages")
    public Slice<MessageDto> getMessages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt")
                .and(Sort.by(Sort.Direction.DESC, "id"));

        Pageable pageable = PageRequest.of(page, size, sort);

        return messageService.getHistory(pageable);
    }

    @GetMapping("/messages/private")
    public Slice<MessageDto> getPrivateMessages(
            @RequestParam Long senderId,
            @RequestParam Long receiverId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt")
                .and(Sort.by(Sort.Direction.DESC, "id"));

        Pageable pageable = PageRequest.of(page, size, sort);

        return messageService.getPrivateHistory(senderId, receiverId, pageable);
    }

    @GetMapping("/messages/unread-counts")
    public List<UnreadCountProjection> getUnreadCounts(Principal principal) {

        String username = principal.getName();

        Long receiverId = userService.getUserIdByUsername(username);

        return messageService.getUnreadCounts(receiverId);
    }
}
