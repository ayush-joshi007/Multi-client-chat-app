package com.chatapp.service;

import com.chatapp.Mapper.impl.MessageMapper;
import com.chatapp.dto.ConversationDto;
import com.chatapp.dto.DeleteMessageRequest;
import com.chatapp.dto.EditMessageRequest;
import com.chatapp.projection.ConversationSummaryProjection;
import com.chatapp.projection.UnreadCountProjection;
import com.chatapp.tracker.ActiveChatTracker;
import com.chatapp.dto.MessageDto;
import com.chatapp.entity.MessageEntity;
import com.chatapp.entity.MessageStatus;
import com.chatapp.entity.UserEntity;
import com.chatapp.repository.MessageRepository;
import com.chatapp.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class MessageService {

    private static final String DELETED_MESSAGE_PLACEHOLDER = "THIS MESSAGE WAS DELETED.";

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final MessageMapper messageMapper;
    private final PresenceService presenceService;
    private final ActiveChatTracker activeChatTracker;
    private final SimpMessagingTemplate messagingTemplate;

    private MessageDto buildClientMessageDto(MessageEntity messageEntity) {
        MessageDto dto = messageMapper.mapTo(messageEntity);

        if (messageEntity.isDeleted()) {
            dto.setContent(DELETED_MESSAGE_PLACEHOLDER);
        }

        return dto;
    }

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

        return buildClientMessageDto(savedMessageEntity);
    }

    public MessageDto editMessage(EditMessageRequest request, Long authenticatedUserId) {

        if (request.getMessageId() == null) {
            throw new IllegalArgumentException("Message id is required");
        }

        String newContent = request.getContent();

        if (newContent == null || newContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }

        MessageEntity message = messageRepository.findById(request.getMessageId())
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        if (message.getSender() == null ||
                message.getSender().getUserId() != authenticatedUserId) {
            throw new SecurityException("You can only edit your own messages");
        }

        if (message.isDeleted()) {
            throw new IllegalArgumentException("Deleted messages cannot be edited");
        }

        String normalizedContent = newContent.trim();

        if (normalizedContent.equals(message.getContent())) {
            throw new IllegalArgumentException("Edited content must be different");
        }

        message.setContent(normalizedContent);
        message.setEdited(true);
        message.setEditedAt(LocalDateTime.now());

        MessageEntity savedMessage = messageRepository.save(message);

        return buildClientMessageDto(savedMessage);
    }

    public MessageDto deleteMessage(DeleteMessageRequest request, Long authenticatedUserId) {

        if (request.getMessageId() == null) {
            throw new IllegalArgumentException("Message id is required");
        }

        MessageEntity message = messageRepository.findById(request.getMessageId())
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        if (message.getSender() == null ||
                message.getSender().getUserId() != authenticatedUserId) {
            throw new SecurityException("You can only delete your own messages");
        }

        if (message.isDeleted()) {
            return buildClientMessageDto(message);
        }

        message.setDeleted(true);
        message.setDeletedAt(LocalDateTime.now());

        MessageEntity savedMessage = messageRepository.save(message);

        return buildClientMessageDto(savedMessage);
    }

    public List<MessageDto> getHistory() {
        Iterable<MessageEntity> result= messageRepository.findByReceiverIsNullOrderByCreatedAt();
        List<MessageDto> li = new ArrayList<>();
        for(MessageEntity m: result){
            MessageDto msgDto = buildClientMessageDto(m);
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

            MessageDto dto = buildClientMessageDto(savedMessage);

            messagingTemplate.convertAndSend(
                    "/topic/user/" + dto.getSenderId(),
                    dto
            );
        }


        List<MessageEntity> result = messageRepository.findPrivateConversation(senderId, receiverId);

        List<MessageDto> messages = new ArrayList<>();

        for(MessageEntity m : result){

            MessageDto msgDto = buildClientMessageDto(m);

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
                    buildClientMessageDto(message)
            );
        }
    }

    public List<UnreadCountProjection> getUnreadCounts(Long receiverId){
        return messageRepository.findUnreadCountsByReceiverId(receiverId);
    }


    public List<ConversationDto> getConversationSummaries(Long currentUserId) {

        List<ConversationSummaryProjection> conversations =
                messageRepository.findConversationSummaries(currentUserId);

        Map<Long, ConversationSummaryProjection> conversationByPartnerId =
                conversations.stream()
                        .collect(Collectors.toMap(
                                ConversationSummaryProjection::getPartnerId,
                                conversation -> conversation
                        ));

        Map<Long, Long> unreadCounts = messageRepository
                .findUnreadCountsByReceiverId(currentUserId)
                .stream()
                .collect(Collectors.toMap(
                        UnreadCountProjection::getSenderId,
                        UnreadCountProjection::getUnreadCount
                ));

        List<ConversationDto> result = new ArrayList<>();

        for (UserEntity user : userRepository.findAll()) {

            ConversationDto dto = new ConversationDto();

            dto.setPartnerId(user.getUserId());
            dto.setPartnerUsername(user.getUserName());
            dto.setOnline(presenceService.isOnline(user.getUserId()));
            dto.setUnreadCount(
                    unreadCounts.getOrDefault(user.getUserId(), 0L)
            );

            ConversationSummaryProjection conversation =
                    conversationByPartnerId.get(user.getUserId());

            if (conversation != null) {
                boolean lastMessageDeleted = Boolean.TRUE.equals(conversation.getLastMessageDeleted());

                dto.setLastMessageDeleted(lastMessageDeleted);
                dto.setLastMessage(
                        lastMessageDeleted
                                ? DELETED_MESSAGE_PLACEHOLDER
                                : conversation.getLastMessage()
                );
                dto.setLastMessageTime(conversation.getLastMessageTime());
                dto.setLastSenderId(conversation.getLastSenderId());
            }

            result.add(dto);
        }

        result.sort((left, right) -> {
            boolean leftIsCurrentUser = left.getPartnerId().equals(currentUserId);
            boolean rightIsCurrentUser = right.getPartnerId().equals(currentUserId);

            if (leftIsCurrentUser && !rightIsCurrentUser) {
                return -1;
            }

            if (!leftIsCurrentUser && rightIsCurrentUser) {
                return 1;
            }

            boolean leftHasMessage = left.getLastMessageTime() != null;
            boolean rightHasMessage = right.getLastMessageTime() != null;

            if (leftHasMessage && rightHasMessage) {
                return right.getLastMessageTime().compareTo(left.getLastMessageTime());
            }

            if (leftHasMessage) {
                return -1;
            }

            if (rightHasMessage) {
                return 1;
            }

            return left.getPartnerUsername().compareToIgnoreCase(right.getPartnerUsername());
        });

        return result;
    }


}
