package net.pistonmaster.pistonfilter.listeners;

import me.xdrop.fuzzywuzzy.FuzzySearch;
import net.md_5.bungee.api.ChatColor;
import net.pistonmaster.pistonchat.api.PistonChatEvent;
import net.pistonmaster.pistonchat.api.PistonWhisperEvent;
import net.pistonmaster.pistonchat.utils.CommonTool;
import net.pistonmaster.pistonchat.utils.UniqueSender;
import net.pistonmaster.pistonfilter.PistonFilter;
import net.pistonmaster.pistonfilter.utils.*;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.StringUtil;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ChatListener implements Listener {
    private final PistonFilter plugin;
    private final Deque<MessageInfo> globalMessages;
    private final Map<UUID, FilteredPlayer> players = new ConcurrentHashMap<>();

    public ChatListener(PistonFilter plugin) {
        this.plugin = plugin;
        this.globalMessages = new MaxSizeDeque<>(plugin.getConfig().getInt("global-message-stack-size"));
    }

    @EventHandler(ignoreCancelled = true)
    public void onQuit(PlayerQuitEvent event) {
        players.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onChat(PistonChatEvent event) {
        handleMessage(event.getPlayer(), MessageInfo.of(Instant.now(), event.getMessage()),
                () -> event.setCancelled(true),
                message -> CommonTool.sendChatMessage(event.getPlayer(), message, event.getPlayer()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onWhisper(PistonWhisperEvent event) {
        if (event.getSender() == event.getReceiver()) return;

        handleMessage(event.getSender(), MessageInfo.of(Instant.now(), event.getMessage()),
                () -> event.setCancelled(true),
                message -> CommonTool.sendSender(event.getSender(), message, event.getReceiver()));
    }

    public void handleMessage(CommandSender sender, MessageInfo message, Runnable cancelEvent, Consumer<String> sendEmpty) {
        if (sender.hasPermission("pistonfilter.bypass")) return;

        for (String str : plugin.getConfig().getStringList("banned-text")) {
            if (FuzzySearch.partialRatio(message.getStrippedMessage(), StringHelper.revertLeet(str)) > plugin.getConfig().getInt("banned-text-partial-ratio")) {
                cancelMessage(sender, message, cancelEvent, sendEmpty, String.format("Contains banned text: %s", str));
                return;
            }
        }

        int wordsWithNumbers = 0;
        for (String word : message.getWords()) {
            if (word.length() > plugin.getConfig().getInt("max-word-length")) {
                cancelMessage(sender, message, cancelEvent, sendEmpty, String.format("Contains a word with length (%d) \"%s\".", word.length(), word));
                return;
            } else if (hasInvalidSeparators(word)) {
                cancelMessage(sender, message, cancelEvent, sendEmpty, String.format("Has a word with invalid separators (%s).", word));
                return;
            }

            if (StringHelper.containsDigit(word)) {
                wordsWithNumbers++;
            }
        }

        if (wordsWithNumbers > plugin.getConfig().getInt("max-words-with-numbers")) {
            cancelMessage(sender, message, cancelEvent, sendEmpty, String.format("Used %d words with numbers.", wordsWithNumbers));
            return;
        }

        if (plugin.getConfig().getBoolean("no-repeat")) {
            UUID uuid = new UniqueSender(sender).getUniqueId();
            FilteredPlayer filteredPlayerCached = players.compute(uuid, (k, v) -> {
                if (v == null) {
                    return new FilteredPlayer(new UniqueSender(sender).getUniqueId(), new MaxSizeDeque<>(
                            plugin.getConfig().getInt("no-repeat-stack-size")));
                } else {
                    return v;
                }
            });
            int noRepeatTime = plugin.getConfig().getInt("no-repeat-time");
            int similarRatio = plugin.getConfig().getInt("no-repeat-similar-ratio");
            Deque<MessageInfo> lastMessages = filteredPlayerCached.getLastMessages();

            boolean blocked = isBlocked(sender, message, cancelEvent, sendEmpty, noRepeatTime, similarRatio, lastMessages, false);

            if (!blocked && plugin.getConfig().getBoolean("global-repeat-check")) {
                blocked = isBlocked(sender, message, cancelEvent, sendEmpty, noRepeatTime, similarRatio, globalMessages, true);
            }

            if (!blocked) {
                filteredPlayerCached.getLastMessages().add(message);
                globalMessages.add(message);
            }
        }
    }

    private boolean isBlocked(CommandSender sender, MessageInfo message,
                              Runnable cancelEvent, Consumer<String> sendEmpty,
                              int noRepeatTime,
                              int similarRatio, Deque<MessageInfo> lastMessages,
                              boolean global) {
        int noRepeatNumberMessages = plugin.getConfig().getInt("no-repeat-number-messages");
        int noRepeatNumberAmount = plugin.getConfig().getInt("no-repeat-number-amount");
        int i = 0;
        int foundDigits = 0;
        for (Iterator<MessageInfo> it = lastMessages.descendingIterator(); it.hasNext(); ) {
            MessageInfo pair = it.next();
            if (!global && message.isContainsDigit() && pair.isContainsDigit()
                    && i < noRepeatNumberMessages) {
                foundDigits++;
            }

            int similarity = -1;
            if (foundDigits >= noRepeatNumberAmount ||
                    (Duration.between(pair.getTime(), message.getTime()).getSeconds() < noRepeatTime
                            && (similarity = FuzzySearch.weightedRatio(pair.getStrippedMessage(), message.getStrippedMessage())) > similarRatio)) {
                String reason = similarity > -1 ?
                        String.format("Similar to previous message (%d%%) (%s).", similarity, message.getOriginalMessage()) : "Contains too many numbers.";
                cancelMessage(sender, message, cancelEvent, sendEmpty, reason);
                return true;
            }
            i++;
        }
        return false;
    }

    private boolean hasInvalidSeparators(String word) {
        //noinspection Since15
        List<Character> chars = word.chars().mapToObj(c -> (char) c).collect(Collectors.toList());
        int maxSeparators = plugin.getConfig().getInt("max-separated-numbers");
        int separators = 0;
        int index = 0;
        for (char c : chars) {
            if (Character.isDigit(c)) {
                if (index >= (chars.size() - 1)) {
                    return false;
                } else if (!Character.isDigit(chars.get(index + 1)) && ++separators > maxSeparators) {
                    return true;
                }
            }
            index++;
        }
        return false;
    }

    private void cancelMessage(CommandSender sender, MessageInfo message, Runnable cancelEvent, Consumer<String> sendEmpty, String reason) {
        cancelEvent.run();

        if (plugin.getConfig().getBoolean("message-sender")) {
            sendEmpty.accept(message.getOriginalMessage());
        }

        if (plugin.getConfig().getBoolean("verbose")) {
            plugin.getLogger().info(ChatColor.RED + "[AntiSpam] <" + sender.getName() + "> " + message.getOriginalMessage() + " (" + reason + ")");
        }
    }
}
