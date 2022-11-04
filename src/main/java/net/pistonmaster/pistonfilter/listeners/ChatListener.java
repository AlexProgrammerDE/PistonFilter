package net.pistonmaster.pistonfilter.listeners;

import me.xdrop.fuzzywuzzy.FuzzySearch;
import net.md_5.bungee.api.ChatColor;
import net.pistonmaster.pistonchat.api.PistonChatEvent;
import net.pistonmaster.pistonchat.api.PistonWhisperEvent;
import net.pistonmaster.pistonchat.utils.CommonTool;
import net.pistonmaster.pistonchat.utils.UniqueSender;
import net.pistonmaster.pistonfilter.PistonFilter;
import net.pistonmaster.pistonfilter.utils.FilteredPlayer;
import net.pistonmaster.pistonfilter.utils.Pair;
import net.pistonmaster.pistonfilter.utils.StringHelper;
import net.pistonmaster.pistonutils.StringUtil;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ChatListener implements Listener {
    private final PistonFilter plugin;
    private final Queue<Pair<Instant, String>> globalMessages;
    private final Map<UUID, FilteredPlayer> players = new ConcurrentHashMap<>();

    public ChatListener(PistonFilter plugin) {
        this.plugin = plugin;
        this.globalMessages = new CircularFifoQueue<>(plugin.getConfig().getInt("global-message-stack-size"));
    }

    @EventHandler(ignoreCancelled = true)
    public void onQuit(PlayerQuitEvent event) {
        players.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onChat(PistonChatEvent event) {
        handleMessage(event.getPlayer(), event.getMessage(),
                () -> event.setCancelled(true),
                message -> CommonTool.sendChatMessage(event.getPlayer(), message, event.getPlayer()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onWhisper(PistonWhisperEvent event) {
        if (event.getSender() == event.getReceiver()) return;

        handleMessage(event.getSender(), event.getMessage(),
                () -> event.setCancelled(true),
                message -> CommonTool.sendSender(event.getSender(), message, event.getReceiver()));
    }

    public void handleMessage(CommandSender sender, String message, Runnable cancelEvent, Consumer<String> sendEmpty) {
        Instant now = Instant.now();
        if (sender.hasPermission("pistonfilter.bypass")) return;

        String[] words = Arrays.stream(message.split(" ")).filter(word -> !word.isEmpty()).toArray(String[]::new);
        String cutMessage = StringHelper.revertLeet(message.toLowerCase().replace(" ", "").replaceAll("\\s+", ""));

        for (String str : plugin.getConfig().getStringList("banned-text")) {
            if (FuzzySearch.partialRatio(cutMessage, StringHelper.revertLeet(str)) > plugin.getConfig().getInt("banned-text-partial-ratio")) {
                cancelMessage(sender, message, cancelEvent, sendEmpty);
                return;
            }
        }

        int wordsWithNumbers = 0;
        for (String word : words) {
            if (word.length() > plugin.getConfig().getInt("max-word-length")
                    || hasInvalidSeparators(word)) {
                cancelMessage(sender, message, cancelEvent, sendEmpty);
                return;
            }

            if (StringHelper.containsDigit(word)) {
                wordsWithNumbers++;
            }
        }

        if (wordsWithNumbers > plugin.getConfig().getInt("max-words-with-numbers")) {
            cancelMessage(sender, message, cancelEvent, sendEmpty);
            return;
        }

        if (plugin.getConfig().getBoolean("no-repeat")) {
            boolean blocked = false;
            UUID uuid = new UniqueSender(sender).getUniqueId();
            FilteredPlayer filteredPlayerCached = players.get(uuid);
            int noRepeatTime = plugin.getConfig().getInt("no-repeat-time");
            int similarRatio = plugin.getConfig().getInt("no-repeat-similar-ratio");
            if (filteredPlayerCached != null) {
                Queue<Pair<Instant, String>> lastMessages = filteredPlayerCached.getLastMessages();

                blocked = isBlocked(sender, message, cancelEvent, sendEmpty, now, cutMessage, noRepeatTime, similarRatio, lastMessages, false);
            }

            if (!blocked && plugin.getConfig().getBoolean("global-repeat-check")) {
                blocked = isBlocked(sender, message, cancelEvent, sendEmpty, now, cutMessage, noRepeatTime, similarRatio, globalMessages, true);
            }

            if (!blocked) {
                if (filteredPlayerCached == null) {
                    filteredPlayerCached = new FilteredPlayer(new UniqueSender(sender).getUniqueId(), new CircularFifoQueue<>(
                            plugin.getConfig().getInt("no-repeat-stack-size")));
                    players.put(uuid, filteredPlayerCached);
                }

                filteredPlayerCached.getLastMessages().add(new Pair<>(now, cutMessage));
                globalMessages.add(new Pair<>(now, cutMessage));
            }
        }
    }

    private boolean isBlocked(CommandSender sender, String message,
                              Runnable cancelEvent, Consumer<String> sendEmpty,
                              Instant now, String cutMessage, int noRepeatTime,
                              int similarRatio, Queue<Pair<Instant, String>> lastMessages,
                              boolean global) {
        boolean containsDigit = StringHelper.containsDigit(cutMessage);
        int noRepeatNumberMessages = plugin.getConfig().getInt("no-repeat-number-messages");
        int noRepeatNumberAmount = plugin.getConfig().getInt("no-repeat-number-amount");
        int i = 0;
        int foundDigits = 0;
        for (Pair<Instant, String> pair : lastMessages) {
            Instant messageTime = pair.getKey();
            String messageText = pair.getValue();

            if (!global && containsDigit && StringHelper.containsDigit(messageText)
                    && i < noRepeatNumberMessages) {
                foundDigits++;
            }

            if (foundDigits >= noRepeatNumberAmount ||
                    (Duration.between(messageTime, now).getSeconds() < noRepeatTime
                            && FuzzySearch.weightedRatio(messageText, cutMessage) > similarRatio)) {
                cancelMessage(sender, message, cancelEvent, sendEmpty);
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
                } else {
                    if (!Character.isDigit(chars.get(index + 1))) {
                        separators++;
                        if (separators > maxSeparators) {
                            return true;
                        }
                    }
                }
            }
            index++;
        }
        return false;
    }

    private void cancelMessage(CommandSender sender, String message, Runnable cancelEvent, Consumer<String> sendEmpty) {
        cancelEvent.run();

        if (plugin.getConfig().getBoolean("message-sender")) {
            sendEmpty.accept(message);
        }

        if (plugin.getConfig().getBoolean("verbose")) {
            plugin.getLogger().info(ChatColor.RED + "[AntiSpam] <" + sender.getName() + "> " + message);
        }
    }
}
