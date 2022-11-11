package net.pistonmaster.pistonfilter.utils;

import lombok.*;

import java.time.Instant;
import java.util.Arrays;

@Getter
@ToString
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MessageInfo {
    private final Instant time;
    private final String originalMessage;
    private final String strippedMessage;
    private final String[] words;
    private final boolean containsDigit;

    public static MessageInfo of(Instant time, String message) {
        return new MessageInfo(
                time,
                message,
                StringHelper.revertLeet(message.toLowerCase().replace(" ", "").replaceAll("\\s+", "")),
                Arrays.stream(message.split(" ")).filter(word -> !word.isEmpty()).toArray(String[]::new),
                message.matches(".*\\d.*")
        );
    }
}
