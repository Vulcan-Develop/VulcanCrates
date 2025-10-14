package net.vulcandev.vulcancrates.command.subcommand;

import net.xantharddev.vulcanlib.libs.Colour;
import org.bukkit.command.CommandSender;
import net.vulcandev.vulcancrates.VulcanCrates;
import net.vulcandev.vulcancrates.objects.Crate;

/**
 * Subcommand to remove a crate's location and hologram.
 * Usage: /crate remove <crate>
 * Removes the chest block and hologram from the world.
 */
public class RemoveCommand {
    private final VulcanCrates plugin;

    public RemoveCommand(VulcanCrates plugin) {this.plugin = plugin;}

    /**
     * Executes the remove command.
     * Removes a crate's location, chest block, and hologram from the world.
     * @param sender the command sender
     * @param args command arguments: crate name
     */
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("crates.remove")) {
            sender.sendMessage(Colour.colour("&cYou don't have permission to use this command."));
            return;
        }
        
        if (args.length < 1) {
            sender.sendMessage(Colour.colour("&cUsage: /crates remove <crate>"));
            return;
        }
        
        Crate crate = plugin.getCrateManager().getCrate(args[0]);
        if (crate == null) {
            sender.sendMessage(Colour.colour("&cCrate '" + args[0] + "' not found."));
            return;
        }
        
        if (crate.getLocation() == null) {
            sender.sendMessage(Colour.colour("&cCrate '" + args[0] + "' doesn't have a location set."));
            return;
        }
        
        // Remove the crate location and chest
        plugin.getCrateManager().removeCrateLocation(args[0]);
        
        String prefix = plugin.conf().getString("messages.prefix", "&6[Crates]");
        String message = plugin.conf().getString("messages.crate-removed",
                "%prefix% &7%crateType% crate location removed and chest destroyed.")
                .replace("%prefix%", prefix)
                .replace("%crateType%", crate.getName());
        
        sender.sendMessage(Colour.colour(message));
    }
}