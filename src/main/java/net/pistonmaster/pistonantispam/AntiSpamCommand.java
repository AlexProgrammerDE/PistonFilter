package net.pistonmaster.pistonantispam;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class AntiSpamCommand extends Command {
    private final PistonAntiSpam plugin;

    public AntiSpamCommand(PistonAntiSpam plugin) {
        super("pistonantispam", "pistonantispam.admin");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender.hasPermission("pistonantispam.admin")) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("reload")) {
                    plugin.reloadConfig();
                }

                if (args[0].equalsIgnoreCase("add")) {
                    if (args.length > 1) {
                        Configuration config = plugin.config;

                        config.set("banned-text", plugin.config.getStringList("banned-text").add(args[1]));

                        try {
                            ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, new File(plugin.getDataFolder(), "config.yml"));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}
