package net.vulcandev.vulcancrates.command.subcommand;

import net.xantharddev.vulcanlib.libs.Colour;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.vulcandev.vulcancrates.VulcanCrates;
import net.vulcandev.vulcancrates.objects.Crate;
import net.vulcandev.vulcancrates.objects.PlayerData;

import java.util.HashSet;
import java.util.Set;

/**
 * Subcommand to give crate keys to all online players.
 * Usage: /crate giveall <crate> <amount>
 * Supports one-per-IP filtering to prevent abuse.
 */
public class GiveAllCommand {
    private final VulcanCrates plugin;

    public GiveAllCommand(VulcanCrates plugin) {
        this.plugin = plugin;
    }

    /**
     * Executes the giveall command.
     * Gives crate keys to all online players with optional IP filtering.
     * @param sender the command sender
     * @param args command arguments: crate, amount (optional)
     */
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("crates.giveall")) {
            sender.sendMessage(Colour.colour("&cYou don't have permission to use this command."));
            return;
        }
        
        if (args.length < 1) {
            sender.sendMessage(Colour.colour("&cUsage: /crates giveall <crate> [amount]"));
            return;
        }
        
        Crate crate = plugin.getCrateManager().getCrate(args[0]);
        if (crate == null) {
            sender.sendMessage(Colour.colour("&cCrate '" + args[0] + "' not found."));
            return;
        }
        
        int amount = 1;
        if (args.length >= 2) {
            try {
                amount = Integer.parseInt(args[1]);
                if (amount <= 0) {
                    sender.sendMessage(Colour.colour("&cAmount must be positive."));
                    return;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(Colour.colour("&cInvalid amount: " + args[1]));
                return;
            }
        }
        
        Set<String> ipSet = new HashSet<>();
        int playersGiven = 0;
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Check one per IP if enabled
            if (plugin.conf().getBoolean("broadcast.one-per-ip", true)) {
                String ip = player.getAddress().getAddress().getHostAddress();
                if (ipSet.contains(ip)) {
                    continue;
                }
                ipSet.add(ip);
            }
            
            PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
            playerData.addKeys(crate.getName(), amount);
            playersGiven++;
        }
        
        String prefix = plugin.conf().getString("messages.prefix", "&6[Crates]");
        
        // Broadcast message if enabled
        if (plugin.conf().getBoolean("broadcast.giveall-enabled", true)) {
            for (String message : plugin.conf().getStringList("broadcast.giveall-message")) {
                String formattedMessage = message
                        .replace("{amount}", String.valueOf(amount))
                        .replace("{crateName}", crate.getName());
                Bukkit.broadcastMessage(Colour.colour(formattedMessage));
            }
        }
        
        // Message to sender
        sender.sendMessage(Colour.colour(prefix + " &7Gave &6" + amount + " &7" + crate.getName() + 
                " key(s) to &6" + playersGiven + " &7players!"));
    }
}