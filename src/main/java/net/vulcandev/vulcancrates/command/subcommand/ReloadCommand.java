package net.vulcandev.vulcancrates.command.subcommand;

import net.xantharddev.vulcanlib.libs.Colour;
import org.bukkit.command.CommandSender;
import net.vulcandev.vulcancrates.VulcanCrates;

/**
 * Subcommand to reload all plugin configuration and crates.
 * Usage: /crate reload
 * Reloads configs, crates, holograms, and reports the time taken.
 */
public class ReloadCommand {
    private final VulcanCrates plugin;

    public ReloadCommand(VulcanCrates plugin) {
        this.plugin = plugin;
    }

    /**
     * Executes the reload command.
     * Reloads all plugin configuration and displays the time taken.
     * @param sender the command sender
     * @param args command arguments (none required)
     */
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("crates.reload")) {
            sender.sendMessage(Colour.colour("&cYou don't have permission to use this command."));
            return;
        }
        
        long startTime = System.currentTimeMillis();
        plugin.onReload();
        long duration = System.currentTimeMillis() - startTime;
        
        String prefix = plugin.conf().getString("messages.prefix", "&6[Crates]");
        sender.sendMessage(Colour.colour(prefix + " &7Plugin reloaded in " + duration + "ms."));
    }
}