package com.chatapp.tracker;


import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Getter
@Component
public class ActiveChatTracker {

    private final Map<Long, Long> activeChats = new HashMap<>();
}
