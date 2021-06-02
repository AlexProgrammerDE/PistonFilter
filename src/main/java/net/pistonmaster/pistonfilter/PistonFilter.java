package net.pistonmaster.pistonfilter;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class PistonFilter extends JavaPlugin {
    FileConfiguration config;

    @Override
    public void onEnable() {
        getLogger().info(ChatColor.AQUA + "Loading config");
        loadConfig();

        getLogger().info(ChatColor.AQUA + "Registering listener");
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        getLogger().info(ChatColor.AQUA + "Registering command");
        getServer().getPluginCommand("pistonfilter").setExecutor(new FilterCommand(this));

        getLogger().info(ChatColor.AQUA + "Done! :D");
    }

    public void loadConfig() {
        if (!getDataFolder().exists())
            getDataFolder().mkdir();

        File file = new File(getDataFolder(), "config.yml");

        if (!file.exists()) {
            try (InputStream in = getResource("config.yml")) {
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        config = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "config.yml"));
    }
}
