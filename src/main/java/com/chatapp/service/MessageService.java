package com.chatapp.service;

import com.chatapp.Mapper.impl.MessageMapper;
import com.chatapp.projection.UnreadCountProjection;
import com.chatapp.tracker.ActiveChatTracker;
import com.chatapp.dto.MessageDto;
import com.chatapp.entity.MessageEntity;
import com.chatapp.entity.MessageStatus;
import com.chatapp.repository.MessageRepository;
import com.chatapp.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final MessageMapper messageMapper;
    private final PresenceService presenceService;
    private final ActiveChatTracker activeChatTracker;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageDto sendMessage(MessageDto messageDto) {

        if (messageDto.getReceiverId() != null) {

            // Receiver is online
            if (presenceService.isOnline(messageDto.getReceiverId())) {

                messageDto.setStatus(MessageStatus.DELIVERED);

                // Receiver is currently viewing sender's chat
                Long openedChat = activeChatTracker
                        .getActiveChats()
                        .get(messageDto.getReceiverId());

                if (openedChat != null &&
                        openedChat.equals(messageDto.getSenderId())) {

                    messageDto.setStatus(MessageStatus.READ);
                }

            } else {

                // Receiver is offline
                messageDto.setStatus(MessageStatus.SENT);
            }
        }

        MessageEntity messageEntity = messageMapper.mapFrom(messageDto);

        if (messageDto.getSenderId() != null) {
            userRepository.findById(messageDto.getSenderId())
                    .ifPresent(messageEntity::setSender);
        }

        if (messageDto.getReceiverId() != null) {
            userRepository.findById(messageDto.getReceiverId())
                    .ifPresent(messageEntity::setReceiver);
        }

        MessageEntity savedMessageEntity = messageRepository.save(messageEntity);

        MessageDto responseDto = messageMapper.mapTo(savedMessageEntity);

        if (savedMessageEntity.getSender() != null) {
            responseDto.setUserName(savedMessageEntity.getSender().getUserName());
            responseDto.setSenderId(savedMessageEntity.getSender().getUserId());
        }

        return responseDto;
    }

    public List<MessageDto> getHistory() {
        Iterable<MessageEntity> result= messageRepository.findByReceiverIsNullOrderByCreatedAt();
        List<MessageDto> li = new ArrayList<>();
        for(MessageEntity m: result){
            MessageDto msgDto = messageMapper.mapTo(m);
            if(m.getSender() != null){
                msgDto.setUserName(m.getSender().getUserName());
                msgDto.setSenderId(m.getSender().getUserId());
            }
            li.add(msgDto);
        }
        return li;
    }

    public List<MessageDto> getPrivateHistory(Long senderId, Long receiverId){

        activeChatTracker.getActiveChats().put(senderId, receiverId);

        List<MessageEntity> deliveredMessages =
                messageRepository.findBySenderUserIdAndReceiverUserIdAndStatus(
                        receiverId,
                        senderId,
                        MessageStatus.DELIVERED
                );

        for (MessageEntity message : deliveredMessages) {

            message.setStatus(MessageStatus.READ);

            MessageEntity savedMessage = messageRepository.save(message);

            MessageDto dto = messageMapper.mapTo(savedMessage);

            dto.setSenderId(savedMessage.getSender().getUserId());
            dto.setReceiverId(savedMessage.getReceiver().getUserId());
            dto.setUserName(savedMessage.getSender().getUserName());

            messagingTemplate.convertAndSend(
                    "/topic/user/" + dto.getSenderId(),
                    dto
            );
        }


        List<MessageEntity> result = messageRepository.findPrivateConversation(senderId, receiverId);

        List<MessageDto> messages = new ArrayList<>();

        for(MessageEntity m : result){

            MessageDto msgDto = messageMapper.mapTo(m);

            if(m.getSender() != null){
                msgDto.setUserName(m.getSender().getUserName());
                msgDto.setSenderId(m.getSender().getUserId());
            }

            if(m.getReceiver() != null){
                msgDto.setReceiverId(m.getReceiver().getUserId());
            }

            messages.add(msgDto);
        }

        return messages;
    }

    public void markPendingMessagesAsDelivered(Long receiverId){
        List<MessageEntity> pendingMessages =
                messageRepository.findByReceiverUserIdAndStatus(
                        receiverId,
                        MessageStatus.SENT
                );

        for (MessageEntity message : pendingMessages) {
            message.setStatus(MessageStatus.DELIVERED);
        }
        messageRepository.saveAll(pendingMessages);

        for (MessageEntity message : pendingMessages) {

            messagingTemplate.convertAndSend(
                    "/topic/user/" + message.getSender().getUserId(),
                    messageMapper.mapTo(message)
            );
        }
    }

    public List<UnreadCountProjection> getUnreadCounts(Long receiverId){
        return messageRepository.findUnreadCountsByReceiverId(receiverId);
    }


}
