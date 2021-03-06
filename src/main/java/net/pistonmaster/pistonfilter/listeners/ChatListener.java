package net.pistonmaster.pistonfilter.listeners;

import lombok.RequiredArgsConstructor;
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

@RequiredArgsConstructor
public class ChatListener implements Listener {
    private final PistonFilter plugin;
    private final Map<UUID, FilteredPlayer> players = new ConcurrentHashMap<>();

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
        String cutMessage = message.toLowerCase().replace(" ", "").replaceAll("\\s+", "");

        for (String str : plugin.getConfig().getStringList("banned-text")) {
            if (Pattern.compile(StringHelper.toLeetPattern(str).toLowerCase()).matcher(cutMessage).find()) {
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

        if (plugin.getConfig().getBoolean("norepeat")) {
            boolean blocked = false;
            UUID uuid = new UniqueSender(sender).getUniqueId();
            FilteredPlayer filteredPlayerCached = players.get(uuid);

            if (filteredPlayerCached != null) {
                Pair<Instant, String> lastMessage = filteredPlayerCached.getLastMessage();

                if (lastMessage != null) {
                    Instant lastMessageTime = lastMessage.getKey();
                    String lastMessageText = lastMessage.getValue();

                    if (Duration.between(lastMessageTime, now).getSeconds() < plugin.getConfig().getInt("norepeat-time")
                        || FuzzySearch.ratio(lastMessageText, cutMessage) > plugin.getConfig().getInt("similarration")) {
                        blocked = true;
                        cancelMessage(sender, message, cancelEvent, sendEmpty);
                    }
                }
            }

            if (!blocked) {
                if (filteredPlayerCached == null) {
                    filteredPlayerCached = new FilteredPlayer(new UniqueSender(sender).getUniqueId());
                    players.put(uuid, filteredPlayerCached);
                }

                filteredPlayerCached.setLastMessage(new Pair<>(Instant.now(), cutMessage));
            }
        }
    }

    private boolean hasInvalidSeparators(String word) {
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
