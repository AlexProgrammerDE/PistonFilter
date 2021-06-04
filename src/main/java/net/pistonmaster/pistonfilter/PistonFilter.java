package net.pistonmaster.pistonfilter;

import net.md_5.bungee.api.ChatColor;
import net.pistonmaster.pistonfilter.commands.FilterCommand;
import net.pistonmaster.pistonfilter.listeners.ChatListener;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

public class PistonFilter extends JavaPlugin {
    @Override
    public void onEnable() {
        getLogger().info(ChatColor.AQUA + "Loading config");
        saveDefaultConfig();

        getLogger().info(ChatColor.AQUA + "Registering commands");
        getServer().getPluginCommand("pistonfilter").setExecutor(new FilterCommand(this));

        getLogger().info(ChatColor.AQUA + "Registering listeners");
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        getLogger().info(ChatColor.AQUA + "Loading metrics");
        new Metrics(this, 11561);

        getLogger().info(ChatColor.AQUA + "Done! :D");
    }
}
