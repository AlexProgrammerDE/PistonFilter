package net.pistonmaster.pistonfilter.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@Getter
public class FilteredPlayer {
    private final UUID id;
    private final Map<String, Instant> messages = new HashMap<>();
}
