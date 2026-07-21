package com.chatapp.projection;

import java.time.LocalDateTime;

public interface ConversationSummaryProjection {

    Long getPartnerId();

    String getPartnerUsername();

    String getLastMessage();

    Boolean getLastMessageDeleted();

    LocalDateTime getLastMessageTime();

    Long getLastSenderId();
}
