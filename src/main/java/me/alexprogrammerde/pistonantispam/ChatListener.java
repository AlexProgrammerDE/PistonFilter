package me.alexprogrammerde.pistonantispam;

import com.sun.tools.javac.util.StringUtils;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class ChatListener implements Listener {
    PistonAntiSpam plugin;

    public ChatListener(PistonAntiSpam plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(ChatEvent event) {
        if (event.getSender() instanceof ProxiedPlayer) {
            for (String str : plugin.config.getStringList("bannedtext")) {
                if (event.getMessage().toLowerCase().contains(str.toLowerCase())) {
                    event.setCancelled(true);
                    ProxiedPlayer sender = (ProxiedPlayer) event.getSender();

                    sender.sendMessage(new ComponentBuilder(event.getMessage()).create());

                    plugin.getLogger().info("Prevented " + sender.getName() + " from saying: " + event.getMessage());

                    break;
                }
            }
        }
    }
}
