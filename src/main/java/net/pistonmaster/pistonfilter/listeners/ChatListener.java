package net.pistonmaster.pistonfilter.listeners;

import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ChatColor;
import net.pistonmaster.pistonchat.api.PistonChatEvent;
import net.pistonmaster.pistonchat.api.PistonWhisperEvent;
import net.pistonmaster.pistonchat.utils.CommonTool;
import net.pistonmaster.pistonfilter.PistonFilter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.regex.Pattern;

@RequiredArgsConstructor
public class ChatListener implements Listener {
    private final PistonFilter plugin;

    public static String toLeetPattern(String str) {
        str = str.toUpperCase();

        char[] english = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};
        String[] leet = {"[A4]", "[B8]", "[C\\(]", "[D\\)]", "[E3]", "[F\\}]", "[G6]", "[H#]", "[I!]", "[J\\]]", "[KX]", "[L|]", "[M]", "[N]", "[O0]", "[P9]", "[Q]", "[R2]", "[SZ]", "[T7]", "[UM]", "[V]", "[W]", "[X]", "[J]", "[Z]"};
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < str.length(); i++) {
            char tmp = str.charAt(i);
            boolean foundMatch = false;

            for (int j = 0; j < english.length; j++) {
                if (tmp == english[j]) {
                    result.append(leet[j]);
                    foundMatch = true;
                    break;
                }
            }

            if (!foundMatch) {
                result.append("\\").append(tmp);
            }
        }

        return result.toString();
    }

    @EventHandler
    public void onChat(PistonChatEvent event) {
        String message = event.getMessage();

        if (event.getPlayer().hasPermission("pistonfilter.bypass")) return;

        String cutMessage = message.toLowerCase().replace(" ", "").replaceAll("\\s+", "");

        for (String str : plugin.getConfig().getStringList("banned-text")) {
            if (Pattern.compile(toLeetPattern(str).toLowerCase()).matcher(cutMessage).find()) {
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
    }

    @EventHandler
    public void onWhisper(PistonWhisperEvent event) {
        String message = event.getMessage();

        if (event.getSender().hasPermission("pistonfilter.bypass")) return;

        String cutMessage = message.toLowerCase().replace(" ", "").replaceAll("\\s+", "");

        for (String str : plugin.getConfig().getStringList("banned-text")) {
            if (Pattern.compile(toLeetPattern(str).toLowerCase()).matcher(cutMessage).find()) {
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
    }
}
