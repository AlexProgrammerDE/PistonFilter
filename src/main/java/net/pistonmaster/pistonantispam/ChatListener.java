package net.pistonmaster.pistonantispam;

import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

@RequiredArgsConstructor
public class ChatListener implements Listener {
    private final PistonAntiSpam plugin;

    @EventHandler
    public void onChat(ChatEvent event) {
        if (event.getSender() instanceof ProxiedPlayer) {
            ProxiedPlayer sender = (ProxiedPlayer) event.getSender();

            if (!sender.hasPermission("pistonantispam.bypass"))
                return;

            if (!plugin.config.getBoolean("ignore-slash") && event.getMessage().startsWith("/"))
                return;

            String cutMessage = event.getMessage().toLowerCase().replace(" ", "");

            for (String str : plugin.config.getStringList("banned-text")) {
                if (cutMessage.contains(str.toLowerCase())) {
                    event.setCancelled(true);

                    if(plugin.config.getBoolean("message-sender") && !event.getMessage().startsWith("/")) {
                        sender.sendMessage(new ComponentBuilder("<" + sender.getDisplayName() + "> " + event.getMessage()).create());
                    }

                    if(plugin.config.getBoolean("verbose")) {
                        plugin.getLogger().info(ChatColor.RED + "Prevented " + sender.getName() + " from saying: " + event.getMessage());
                    }

                    break;
                }
            }
        }
    }
}
