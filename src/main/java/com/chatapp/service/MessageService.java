package com.chatapp.service;

import com.chatapp.Mapper.impl.MessageMapper;
import com.chatapp.dto.MessageDto;
import com.chatapp.entity.MessageEntity;
import com.chatapp.repository.MessageRepository;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Service
public class MessageService {

    private final MessageRepository messageRepository;

    private final MessageMapper messageMapper;
    private final ModelMapper modelMapper;

    public MessageDto sendMessage(MessageDto messageDto){
        MessageEntity messageEntity = messageMapper.mapFrom(messageDto);
        MessageEntity savedMessageEntity =  messageRepository.save(messageEntity);
        return messageMapper.mapTo(savedMessageEntity);
    }

    public List<MessageDto> getHistory() {
        Iterable<MessageEntity> result= messageRepository.findAll();
        List<MessageDto> li = new ArrayList<>();
        for(MessageEntity m: result){
            MessageDto msgDto = messageMapper.mapTo(m);
            li.add(msgDto);
        }
        return li;
    }




//    public List<MessageEntity> getChatHistory(Long senderId, Long receiverId){
//        Iterable<MessageEntity> result= messageRepository.findBySenderIdAndReceiverId(senderId, receiverId);
//        Iterable<MessageEntity> reverseResult= messageRepository.findBySenderIdAndReceiverId(receiverId, senderId);
//        List<MessageEntity> chatHistory = new ArrayList<>();
//        result.forEach(chatHistory::add);
//        reverseResult.forEach(chatHistory::add);
//        chatHistory.sort((m1, m2) -> m1.getCreatedAt().compareTo(m2.getCreatedAt()));
//        return chatHistory;
//
//    }
}
