package com.chatapp.service;

import com.chatapp.entity.MessageEntity;
import com.chatapp.repository.MessageRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class MessageService {

    private MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public MessageEntity sendMessage(MessageEntity messageEntity){
        MessageEntity savedMessage = messageRepository.save(messageEntity);
        return savedMessage;
    }

    public List<MessageEntity> getChatHistory(Long senderId, Long receiverId){
        Iterable<MessageEntity> result= messageRepository.findBySenderIdAndReceiverId(senderId, receiverId);
        Iterable<MessageEntity> reverseResult= messageRepository.findBySenderIdAndReceiverId(receiverId, senderId);
        List<MessageEntity> chatHistory = new ArrayList<>();
        result.forEach(chatHistory::add);
        reverseResult.forEach(chatHistory::add);
        chatHistory.sort((m1, m2) -> m1.getCreatedAt().compareTo(m2.getCreatedAt()));
        return chatHistory;

    }
}
