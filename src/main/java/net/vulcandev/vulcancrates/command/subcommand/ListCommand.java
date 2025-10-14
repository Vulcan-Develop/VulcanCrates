package net.vulcandev.vulcancrates.command.subcommand;

import net.xantharddev.vulcanlib.libs.Colour;
import org.bukkit.command.CommandSender;
import net.vulcandev.vulcancrates.VulcanCrates;
import net.vulcandev.vulcancrates.objects.Crate;

import java.util.Collection;

/**
 * Subcommand to list all available crates and their locations.
 * Usage: /crate list
 * Displays all crates, their locations, and required permissions.
 */
public class ListCommand {

    private final VulcanCrates plugin;

    public ListCommand(VulcanCrates plugin) {this.plugin = plugin;}

    /**
     * Executes the list command.
     * Displays all crates with their placement status and permissions.
     * @param sender the command sender
     * @param args command arguments (none required)
     */
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("crates.list")) {
            sender.sendMessage(Colour.colour("&cYou don't have permission to use this command."));
            return;
        }

        Collection<Crate> crates = plugin.getCrateManager().getAllCrates();

        if (crates.isEmpty()) {
            sender.sendMessage(Colour.colour("&cNo crates found."));
            return;
        }

        String prefix = plugin.conf().getString("messages.prefix", "&6[Crates]");
        sender.sendMessage(Colour.colour(prefix + " &7Available crates:"));
        sender.sendMessage(Colour.colour("&7&m----------------------------"));

        for (Crate crate : crates) {
            String crateName = crate.getName();
            String displayName = crate.getDisplayName();

            // Check if crate has a location set
            String locationStatus;
            if (crate.getLocation() != null) {
                locationStatus = "&aPlaced at " + crate.getLocation().getWorldName() +
                               " (" + crate.getLocation().getX() + ", " +
                               crate.getLocation().getY() + ", " +
                               crate.getLocation().getZ() + ")";
            } else {
                locationStatus = "&7Not placed";
            }

            // Build permission info
            String previewPerm = "crates.preview";
            String usePerm = "crates.use." + crateName.toLowerCase();

            sender.sendMessage(Colour.colour("&e" + crateName + " &8(" + displayName + "&8)"));
            sender.sendMessage(Colour.colour("  &7Status: " + locationStatus));
            sender.sendMessage(Colour.colour("  &7Preview Permission: &f" + previewPerm));
            sender.sendMessage(Colour.colour("  &7Use Permission: &f" + usePerm));
            sender.sendMessage(Colour.colour(""));
        }

        sender.sendMessage(Colour.colour("&7Total crates: &e" + crates.size()));
    }
}