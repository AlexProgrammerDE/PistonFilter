package net.pistonmaster.pistonfilter.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Getter
public class FilteredPlayer {
    private final UUID id;
    @Setter
    private volatile Pair<Instant, String> lastMessage;
}
