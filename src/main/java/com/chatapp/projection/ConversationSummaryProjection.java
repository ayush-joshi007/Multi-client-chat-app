package com.chatapp.projection;

import java.time.LocalDateTime;

public interface ConversationSummaryProjection {

    Long getPartnerId();

    String getPartnerUsername();

    String getLastMessage();

    LocalDateTime getLastMessageTime();

    Long getLastSenderId();
}
