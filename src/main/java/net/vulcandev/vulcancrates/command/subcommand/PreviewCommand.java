package net.vulcandev.vulcancrates.command.subcommand;

import net.xantharddev.vulcanlib.libs.Colour;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.vulcandev.vulcancrates.VulcanCrates;
import net.vulcandev.vulcancrates.gui.CratePreviewGUI;
import net.vulcandev.vulcancrates.objects.Crate;

/**
 * Subcommand to open a GUI preview of a crate's prizes.
 * Usage: /crate preview <crate>
 * Opens an inventory GUI showing all available prizes.
 */
public class PreviewCommand {

    private final VulcanCrates plugin;

    public PreviewCommand(VulcanCrates plugin) {
        this.plugin = plugin;
    }

    /**
     * Executes the preview command.
     * Opens a GUI inventory showing all prizes for the specified crate.
     * @param sender the command sender (must be a player)
     * @param args command arguments: crate name
     */
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("crates.preview")) {
            sender.sendMessage(Colour.colour("&cYou don't have permission to use this command."));
            return;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(Colour.colour("&cThis command can only be used by players."));
            return;
        }
        
        if (args.length < 1) {
            sender.sendMessage(Colour.colour("&cUsage: /crates preview <crate>"));
            return;
        }
        
        Player player = (Player) sender;
        Crate crate = plugin.getCrateManager().getCrateIgnoreCase(args[0]);
        if (crate == null) {
            sender.sendMessage(Colour.colour("&cCrate '" + args[0] + "' not found."));
            return;
        }
        
        new CratePreviewGUI(plugin, player, crate).open();
    }
}