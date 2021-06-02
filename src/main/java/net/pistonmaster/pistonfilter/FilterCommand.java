package net.pistonmaster.pistonfilter;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public class FilterCommand implements CommandExecutor {
    private final PistonFilter plugin;

    public FilterCommand(PistonFilter plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender.hasPermission("pistonfilter.admin")) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("reload")) {
                    plugin.loadConfig();
                    sender.sendMessage(TextComponent.fromLegacyText(ChatColor.GOLD + "Reloaded the config!"));
                }

                if (args[0].equalsIgnoreCase("add")) {
                    if (args.length > 1) {
                        FileConfiguration config = plugin.config;

                        config.set("banned-text", plugin.config.getStringList("banned-text").add(args[1]));

                        try {
                            config.save(new File(plugin.getDataFolder(), "config.yml"));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        sender.sendMessage(TextComponent.fromLegacyText(ChatColor.GOLD + "Successfully added the config entry!"));
                    }
                }
            }
        }

        return false;
    }
}
