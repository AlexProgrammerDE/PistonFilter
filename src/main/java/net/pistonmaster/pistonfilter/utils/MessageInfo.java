package net.pistonmaster.pistonfilter.utils;

import lombok.*;
import net.md_5.bungee.api.ChatColor;

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
    private final String[] strippedWords;
    private final boolean containsDigit;

    public static MessageInfo of(Instant time, String message) {
        String[] words = Arrays.stream(message.split("\\s+")).filter(word -> !word.isBlank()).toArray(String[]::new);

        return new MessageInfo(
                time,
                message,
                StringHelper.revertLeet(removeWhiteSpace(removeColorCodes(message.replace(" ", "")))),
                words,
                Arrays.stream(words)
                        .map(MessageInfo::removeColorCodes)
                        .map(MessageInfo::removeWhiteSpace)
                        .map(StringHelper::revertLeet).toArray(String[]::new),
                message.matches(".*\\d.*")
        );
    }

    private static String removeWhiteSpace(String string) {
        return string.replaceAll("\\s+", "");
    }

    private static String removeColorCodes(String string) {
        return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', string));
    }
}
