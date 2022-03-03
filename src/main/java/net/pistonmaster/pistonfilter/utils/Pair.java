package net.pistonmaster.pistonfilter.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Pair<K, V> {
    private final K key;
    private final V value;
}
