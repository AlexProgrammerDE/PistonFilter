package net.pistonmaster.pistonfilter;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class FilterCommand extends Command {
    private final PistonFilter plugin;

    public FilterCommand(PistonFilter plugin) {
        super("pistonfilter", "pistonfilter.admin");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender.hasPermission("pistonfilter.admin")) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("reload")) {
                    plugin.loadConfig();
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
