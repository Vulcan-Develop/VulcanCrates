package net.vulcandev.vulcancrates.command.subcommand;

import net.xantharddev.vulcanlib.libs.Colour;
import net.xantharddev.vulcanlib.libs.SerializableLocation;
import net.xantharddev.vulcanlib.libs.material.MaterialDb;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.vulcandev.vulcancrates.VulcanCrates;
import net.vulcandev.vulcancrates.objects.Crate;

/**
 * Subcommand to place a crate at the player's current location.
 * Usage: /crate place <crate>
 * Places a chest block and creates a hologram at the location.
 */
public class PlaceCommand {
    private final VulcanCrates plugin;

    public PlaceCommand(VulcanCrates plugin) {
        this.plugin = plugin;
    }

    /**
     * Executes the place command.
     * Places a crate at the player's current location with a chest block and hologram.
     * @param sender the command sender (must be a player)
     * @param args command arguments: crate name
     */
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("crates.place")) {
            sender.sendMessage(Colour.colour("&cYou don't have permission to use this command."));
            return;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(Colour.colour("&cThis command can only be used by players."));
            return;
        }
        
        if (args.length < 1) {
            sender.sendMessage(Colour.colour("&cUsage: /crates place <crate>"));
            return;
        }
        
        Player player = (Player) sender;
        Crate crate = plugin.getCrateManager().getCrate(args[0]);
        if (crate == null) {
            sender.sendMessage(Colour.colour("&cCrate '" + args[0] + "' not found."));
            return;
        }
        
        // Get the block the player is looking at or standing on
        Block targetBlock = player.getLocation().getBlock();
        
        // Check if the block is replaceable or if we should place on top
        if (targetBlock.getType() != Material.AIR) {
            targetBlock = targetBlock.getRelative(BlockFace.UP);
        }
        
        // Ensure the target location is safe to place a chest
        if (targetBlock.getType() != Material.AIR && 
            targetBlock.getType() != Material.GRASS) {
            player.sendMessage(Colour.colour("&cCannot place crate here - location is obstructed!"));
            return;
        }
        
        // Set crate location to the target block location
        SerializableLocation location = new SerializableLocation(targetBlock.getLocation());

        // Place the chest immediately
        targetBlock.setType(MaterialDb.get(plugin.conf().getString("crate-block.material")));

        // Use the plugin's method to set location (this handles holograms automatically)
        plugin.setCrateLocation(crate.getName(), location);
        
        String prefix = plugin.conf().getString("messages.prefix", "&6[Crates]");
        String message = plugin.conf().getString("messages.crate-block-placed",
                "%prefix% &7%crateType% crate successfully placed at your location.")
                .replace("%prefix%", prefix)
                .replace("%crateType%", crate.getName());
        
        player.sendMessage(Colour.colour(message));
    }
}