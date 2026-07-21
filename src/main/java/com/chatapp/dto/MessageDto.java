package com.chatapp.dto;

import com.chatapp.entity.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageDto {
    private Long id;
    private String content;
    private Long senderId;
    private Long receiverId;
    private String userName;
    private LocalDateTime createdAt;
    private boolean edited;
    private LocalDateTime editedAt;
    private boolean deleted;
    private LocalDateTime deletedAt;
    private MessageStatus status;

}
