package net.pistonmaster.pistonfilter.utils;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentLinkedDeque;

@RequiredArgsConstructor
public class MaxSizeDeque<C> extends ConcurrentLinkedDeque<C> {
    private final int maxSize;

    @Override
    public boolean add(@NotNull C c) {
        if (size() == maxSize) {
            removeFirst();
        }

        return super.add(c);
    }
}
