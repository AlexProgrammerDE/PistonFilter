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
import net.pistonmaster.pistonfilter.utils.StringHelper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class ChatListener implements Listener {
    private final PistonFilter plugin;
    private final List<FilteredPlayer> players = new ArrayList<>();

    @EventHandler
    public void onChat(PistonChatEvent event) {
        String message = event.getMessage();

        if (event.getPlayer().hasPermission("pistonfilter.bypass")) return;

        String cutMessage = message.toLowerCase().replace(" ", "").replaceAll("\\s+", "");

        for (String str : plugin.getConfig().getStringList("banned-text")) {
            if (Pattern.compile(StringHelper.toLeetPattern(str).toLowerCase()).matcher(cutMessage).find()) {
                event.setCancelled(true);

                if (plugin.getConfig().getBoolean("message-sender")) {
                    CommonTool.sendChatMessage(event.getPlayer(), event.getMessage(), event.getPlayer());
                }

                if (plugin.getConfig().getBoolean("verbose")) {
                    plugin.getLogger().info(ChatColor.RED + "[AntiSpam] <" + event.getPlayer().getName() + "> " + event.getMessage());
                }

                break;
            }
        }

        if (!event.isCancelled() && plugin.getConfig().getBoolean("norepeat", true)) {
            boolean blocked = false;
            FilteredPlayer filteredPlayerCached = null;
            for (FilteredPlayer filteredPlayer : players) {
                if (filteredPlayer.getId().equals(event.getPlayer().getUniqueId())) {
                    filteredPlayerCached = filteredPlayer;

                    for (Map.Entry<String, Instant> entry : filteredPlayer.getMessages().entrySet()) {
                        if (Duration.between(entry.getValue(), Instant.now()).getSeconds() < plugin.getConfig().getInt("norepeatmaxdelay", 15)
                                && FuzzySearch.ratio(entry.getKey(), cutMessage) > plugin.getConfig().getInt("similarration", 90)) {
                            blocked = true;
                            event.setCancelled(true);
                        }
                    }
                }
            }

            if (!blocked) {
                if (filteredPlayerCached == null) {
                    filteredPlayerCached = new FilteredPlayer(event.getPlayer().getUniqueId());
                    players.add(filteredPlayerCached);
                }

                filteredPlayerCached.getMessages().put(cutMessage, Instant.now());
            }
        }
    }

    @EventHandler
    public void onWhisper(PistonWhisperEvent event) {
        String message = event.getMessage();

        if (event.getSender() == event.getReceiver()) return;

        if (event.getSender().hasPermission("pistonfilter.bypass")) return;

        String cutMessage = message.toLowerCase().replace(" ", "").replaceAll("\\s+", "");

        for (String str : plugin.getConfig().getStringList("banned-text")) {
            if (Pattern.compile(StringHelper.toLeetPattern(str).toLowerCase()).matcher(cutMessage).find()) {
                event.setCancelled(true);

                if (plugin.getConfig().getBoolean("message-sender")) {
                    CommonTool.sendSender(event.getSender(), message, event.getReceiver());
                }

                if (plugin.getConfig().getBoolean("verbose")) {
                    plugin.getLogger().info(ChatColor.RED + "[AntiSpam] <" + event.getSender().getName() + "> " + event.getMessage());
                }

                break;
            }
        }

        if (!event.isCancelled() && plugin.getConfig().getBoolean("norepeat", true)) {
            boolean blocked = false;
            FilteredPlayer filteredPlayerCached = null;
            for (FilteredPlayer filteredPlayer : players) {
                if (filteredPlayer.getId().equals(new UniqueSender(event.getSender()).getUniqueId())) {
                    filteredPlayerCached = filteredPlayer;

                    for (Map.Entry<String, Instant> entry : filteredPlayer.getMessages().entrySet()) {
                        if (Duration.between(entry.getValue(), Instant.now()).getSeconds() < plugin.getConfig().getInt("norepeatmaxdelay", 15)
                                && FuzzySearch.ratio(entry.getKey(), cutMessage) > plugin.getConfig().getInt("similarration", 90)) {
                            blocked = true;
                            event.setCancelled(true);
                        }
                    }
                }
            }

            if (!blocked) {
                if (filteredPlayerCached == null) {
                    filteredPlayerCached = new FilteredPlayer(new UniqueSender(event.getSender()).getUniqueId());
                    players.add(filteredPlayerCached);
                }

                filteredPlayerCached.getMessages().put(cutMessage, Instant.now());
            }
        }
    }
}
