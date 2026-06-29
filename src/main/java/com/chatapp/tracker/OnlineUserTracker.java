package com.chatapp.tracker;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Getter
@Component
public class OnlineUserTracker {

    private final Set<Long> onlineUsers =
            new HashSet<>();

    private final Map<String, Long> sessionToUser =
            new HashMap<>();

}
