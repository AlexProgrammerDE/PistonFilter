package net.pistonmaster.pistonfilter;

import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.regex.Pattern;

@RequiredArgsConstructor
public class ChatListener implements Listener {
    private final PistonFilter plugin;

    @EventHandler
    public void onChat(ChatEvent event) {
        if (event.getSender() instanceof ProxiedPlayer) {
            ProxiedPlayer sender = (ProxiedPlayer) event.getSender();
            String message = event.getMessage();

            if (sender.hasPermission("pistonfilter.bypass")) return;

            if (message.startsWith("/") && plugin.config.getBoolean("ignore-slash")) return;

            String cutMessage = message.toLowerCase().replace(" ", "").replaceAll("\\s+", "");

            for (String str : plugin.config.getStringList("banned-text")) {
                if (Pattern.compile(toLeetPattern(str).toLowerCase()).matcher(cutMessage).find()) {
                    event.setCancelled(true);

                    if (plugin.config.getBoolean("message-sender") && !event.getMessage().startsWith("/")) {
                        sender.sendMessage(new ComponentBuilder("<" + sender.getDisplayName() + "> " + event.getMessage()).create());
                    }

                    if (plugin.config.getBoolean("verbose")) {
                        String string = (message.startsWith("/")) ? " " : " <" + sender.getName() + "> ";

                        plugin.getLogger().info(ChatColor.RED + "[AntiSpam]" + string + event.getMessage());
                    }

                    break;
                }
            }
        }
    }

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
}
