package net.vulcandev.vulcancrates.command.subcommand;

import net.xantharddev.vulcanlib.libs.Colour;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.vulcandev.vulcancrates.VulcanCrates;
import net.vulcandev.vulcancrates.objects.Crate;
import net.vulcandev.vulcancrates.objects.PlayerData;

/**
 * Subcommand to give crate keys to specific players.
 */
public class GiveCommand {

    private final VulcanCrates plugin;

    public GiveCommand(VulcanCrates plugin) {this.plugin = plugin;}

    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("crates.give")) {
            sender.sendMessage(Colour.colour("&cYou don't have permission to use this command."));
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage(Colour.colour("&cUsage: /crates give <player> <crate> [amount]"));
            return;
        }
        
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(Colour.colour("&cPlayer '" + args[0] + "' not found."));
            return;
        }
        
        Crate crate = plugin.getCrateManager().getCrate(args[1]);
        if (crate == null) {
            sender.sendMessage(Colour.colour("&cCrate '" + args[1] + "' not found."));
            return;
        }
        
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount <= 0) {
                    sender.sendMessage(Colour.colour("&cAmount must be positive."));
                    return;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(Colour.colour("&cInvalid amount: " + args[2]));
                return;
            }
        }
        
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(target);
        playerData.addKeys(crate.getName(), amount);

        String prefix = plugin.conf().getString("messages.prefix", "&6[Crates]");

        String gaveMessage = plugin.conf().getString("messages.gave-keys",
                "%prefix% &7You gave %player% &6%amount% &7%crateType% key(s)!")
                .replace("%prefix%", prefix)
                .replace("%player%", target.getName())
                .replace("%amount%", String.valueOf(amount))
                .replace("%crateType%", crate.getName());
        sender.sendMessage(Colour.colour(gaveMessage));

        String receivedMessage = plugin.conf().getString("messages.received-keys",
                "%prefix% &7You now have &6%amount% &7%crateType% key(s)!")
                .replace("%prefix%", prefix)
                .replace("%amount%", String.valueOf(playerData.getKeys(crate.getName())))
                .replace("%crateType%", crate.getName());
        target.sendMessage(Colour.colour(receivedMessage));
    }
}