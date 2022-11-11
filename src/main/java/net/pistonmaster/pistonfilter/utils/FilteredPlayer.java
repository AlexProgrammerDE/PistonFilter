package net.pistonmaster.pistonfilter.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Getter
public class FilteredPlayer {
    private final UUID id;
    private final Deque<MessageInfo> lastMessages;
}
