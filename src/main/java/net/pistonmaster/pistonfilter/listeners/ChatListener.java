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

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class ChatListener implements Listener {
    private final PistonFilter plugin;
    private final List<FilteredPlayer> players = new ArrayList<>();

    @EventHandler(ignoreCancelled = true)
    public void onChat(PistonChatEvent event) {
        handleMessage(event.getPlayer(), event.getMessage(), () -> event.setCancelled(true));
    }

    @EventHandler(ignoreCancelled = true)
    public void onWhisper(PistonWhisperEvent event) {
        if (event.getSender() == event.getReceiver()) return;

        handleMessage(event.getSender(), event.getMessage(), () -> event.setCancelled(true));
    }

    public void handleMessage(CommandSender sender, String message, Runnable cancelEvent) {
        Instant now = Instant.now();
        if (sender.hasPermission("pistonfilter.bypass")) return;

        String[] words = Arrays.stream(message.split(" ")).filter(word -> !word.isEmpty()).toArray(String[]::new);
        String cutMessage = message.toLowerCase().replace(" ", "").replaceAll("\\s+", "");

        for (String str : plugin.getConfig().getStringList("banned-text")) {
            if (Pattern.compile(StringHelper.toLeetPattern(str).toLowerCase()).matcher(cutMessage).find()) {
                cancelMessage(sender, message, cancelEvent);
                return;
            }
        }

        int wordsWithNumbers = 0;
        for (String word : words) {
            if (word.matches("[0-9]+")) {
                wordsWithNumbers++;
            }
        }

        if (wordsWithNumbers > plugin.getConfig().getInt("max-words-with-numbers")) {
            cancelMessage(sender, message, cancelEvent);
            return;
        }

        if (plugin.getConfig().getBoolean("norepeat")) {
            boolean blocked = false;
            UUID uuid = new UniqueSender(sender).getUniqueId();
            FilteredPlayer filteredPlayerCached = players.stream()
                    .filter(filteredPlayerEntry -> filteredPlayerEntry.getId().equals(uuid))
                    .findFirst()
                    .orElse(null);

            if (filteredPlayerCached != null) {
                Pair<Instant, String> lastMessage = filteredPlayerCached.getLastMessage();

                if (lastMessage != null) {
                    Instant lastMessageTime = lastMessage.getKey();
                    String lastMessageText = lastMessage.getValue();

                    if (Duration.between(lastMessageTime, now).getSeconds() < plugin.getConfig().getInt("norepeat-time")) {
                        blocked = true;
                        cancelMessage(sender, message, cancelEvent);
                    } else if (FuzzySearch.ratio(lastMessageText, cutMessage) > plugin.getConfig().getInt("similarration")) {
                        blocked = true;
                        cancelMessage(sender, message, cancelEvent);
                    }
                }
            }

            if (!blocked) {
                if (filteredPlayerCached == null) {
                    filteredPlayerCached = new FilteredPlayer(new UniqueSender(sender).getUniqueId());
                    players.add(filteredPlayerCached);
                }

                filteredPlayerCached.setLastMessage(new Pair<>(Instant.now(), cutMessage));
            }
        }
    }

    private void cancelMessage(CommandSender sender, String message, Runnable cancelEvent) {
        cancelEvent.run();

        if (plugin.getConfig().getBoolean("message-sender")) {
            CommonTool.sendSender(sender, message, sender);
        }

        if (plugin.getConfig().getBoolean("verbose")) {
            plugin.getLogger().info(ChatColor.RED + "[AntiSpam] <" + sender.getName() + "> " + message);
        }
    }
}
