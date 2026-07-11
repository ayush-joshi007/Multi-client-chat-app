package com.chatapp.tracker;


import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Component
public class ActiveChatTracker {

    private final Map<Long, Long> activeChats = new ConcurrentHashMap<>();
}
