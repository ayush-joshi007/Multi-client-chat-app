package com.chatapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDto {

    private Long partnerId;
    private String partnerUsername;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private Long lastSenderId;
    private boolean online;
    private long unreadCount;
}
