package me.alexprogrammerde.pistonantispam;

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
            String cutMessage= event.getMessage().toLowerCase().replace(" ", "");

            for (String str : plugin.config.getStringList("bannedtext")) {
                if (cutMessage.contains(str.toLowerCase())) {
                    event.setCancelled(true);
                    ProxiedPlayer sender = (ProxiedPlayer) event.getSender();

                    sender.sendMessage(new ComponentBuilder("<" + sender.getDisplayName() + "> " + event.getMessage()).create());

                    plugin.getLogger().info(ChatColor.RED + "Prevented " + sender.getName() + " from saying: " + event.getMessage());

                    break;
                }
            }
        }
    }
}
