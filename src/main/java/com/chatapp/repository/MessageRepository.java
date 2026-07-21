package com.chatapp.repository;

import com.chatapp.entity.MessageEntity;
import com.chatapp.entity.MessageStatus;
import com.chatapp.projection.ConversationSummaryProjection;
import com.chatapp.projection.UnreadCountProjection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends CrudRepository<MessageEntity, Long> {

    Iterable<MessageEntity> findByReceiverIsNullOrderByCreatedAt();

    @Query("""
        SELECT m
        FROM MessageEntity m
        WHERE
            (m.sender.userId = :user1Id
             AND m.receiver.userId = :user2Id)
        OR
            (m.sender.userId = :user2Id
             AND m.receiver.userId = :user1Id)
        ORDER BY m.createdAt
    """)
    List<MessageEntity> findPrivateConversation(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);


    List<MessageEntity> findBySenderUserIdAndReceiverUserIdAndStatus(
            Long senderId,
            Long receiverId,
            MessageStatus status
    );

    List<MessageEntity> findByReceiverUserIdAndStatus(
            Long receiverId,
            MessageStatus status
    );

    @Query("""
        SELECT
            m.sender.userId AS senderId,
            COUNT(m) AS unreadCount
        FROM MessageEntity m
        WHERE
            m.receiver.userId = :receiverId
            AND m.status <> com.chatapp.entity.MessageStatus.READ
        GROUP BY m.sender.userId
    """)
    List<UnreadCountProjection> findUnreadCountsByReceiverId(@Param("receiverId") Long receiverId);


    @Query(value = """
        SELECT
            ranked.partner_id AS "partnerId",
            u.user_name AS "partnerUsername",
            ranked.content AS "lastMessage",
            ranked.deleted AS "lastMessageDeleted",
            ranked.created_at AS "lastMessageTime",
            ranked.sender_id AS "lastSenderId"
        FROM (
            SELECT
                m.*,
                CASE
                    WHEN m.sender_id = :currentUserId THEN m.receiver_id
                    ELSE m.sender_id
                END AS partner_id,
                ROW_NUMBER() OVER (
                    PARTITION BY
                        CASE
                            WHEN m.sender_id = :currentUserId THEN m.receiver_id
                            ELSE m.sender_id
                        END
                    ORDER BY m.created_at DESC, m.id DESC
                ) AS row_num
            FROM messages m
            WHERE
                m.receiver_id IS NOT NULL
                AND (
                    m.sender_id = :currentUserId
                    OR m.receiver_id = :currentUserId
                )
        ) ranked
        JOIN users u
            ON u.user_id = ranked.partner_id
        WHERE ranked.row_num = 1
        ORDER BY ranked.created_at DESC, ranked.id DESC
    """, nativeQuery = true)
    List<ConversationSummaryProjection> findConversationSummaries(
            @Param("currentUserId") Long currentUserId
    );


}
