package net.vulcandev.vulcancrates.command;

import net.vulcandev.vulcancrates.VulcanCrates;
import net.vulcandev.vulcancrates.gui.CratePreviewGUI;
import net.vulcandev.vulcancrates.objects.Crate;
import net.vulcandev.vulcancrates.objects.PlayerData;
import net.xantharddev.vulcanlib.command.SubCommand;
import net.xantharddev.vulcanlib.command.VulcanCommand;
import net.xantharddev.vulcanlib.command.args.ArgumentType;
import net.xantharddev.vulcanlib.command.args.CommandArgument;
import net.xantharddev.vulcanlib.libs.Colour;
import net.xantharddev.vulcanlib.libs.SerializableLocation;
import net.xantharddev.vulcanlib.libs.SimpleBlock;
import net.xantharddev.vulcanlib.libs.material.MaterialDb;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Consolidated crate command handler using VulcanLib's VulcanCommand system.
 * <p>
 * Commands:
 *   /crate give <player> <crate> [amount]  - Give crate keys to a specific player
 *   /crate giveall <crate> <amount>        - Give crate keys to all online players
 *   /crate list                            - List all available crates
 *   /crate place <crate>                   - Place a crate at your location
 *   /crate preview <crate>                 - Preview crate contents in a GUI
 *   /crate reload                          - Reload plugin configuration
 *   /crate remove <crate>                  - Remove a placed crate
 */
public class CrateCommands {

    /**
     * Registers all crate commands with the plugin.
     *
     * @param plugin The VulcanCrates plugin instance
     */
    public static void registerAll(VulcanCrates plugin) {
        VulcanCommand.create("crate")
                .alias("vulcancrates", "crates", "cratesvulcan", "vcrates")
                .description("Manage crates and keys")
                .permission("crates.use")

                .subCommand(SubCommand.create("give")
                        .description("Give crate keys to a player")
                        .permission("crates.give")
                        .argument(CommandArgument.of("player", ArgumentType.PLAYER)
                                .description("The player to give keys to")
                                .required()
                                .build())
                        .argument(CommandArgument.of("crate", ArgumentType.STRING)
                                .description("The type of crate")
                                .required()
                                .completer((sender, partial) -> new ArrayList<>(plugin.getCrateManager().getCrateNames()))
                                .build())
                        .argument(CommandArgument.of("amount", ArgumentType.INT)
                                .description("Number of keys to give (default: 1)")
                                .build())
                        .execute((sender, ctx) -> {
                            Player target = ctx.getPlayer("player");
                            String crateName = ctx.getString("crate");
                            int amount = ctx.getInt("amount", 1);

                            Crate crate = plugin.getCrateManager().getCrateIgnoreCase(crateName);
                            if (crate == null) {
                                sender.sendMessage(Colour.colour("&cCrate '" + crateName + "' not found."));
                                return;
                            }

                            if (amount <= 0) {
                                sender.sendMessage(Colour.colour("&cAmount must be positive."));
                                return;
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
                        })
                        .build())

                .subCommand(SubCommand.create("giveall")
                        .description("Give crate keys to all online players")
                        .permission("crates.giveall")
                        .argument(CommandArgument.of("crate", ArgumentType.STRING)
                                .description("The type of crate")
                                .required()
                                .completer((sender, partial) -> new ArrayList<>(plugin.getCrateManager().getCrateNames()))
                                .build())
                        .argument(CommandArgument.of("amount", ArgumentType.INT)
                                .description("Number of keys to give")
                                .required()
                                .build())
                        .execute((sender, ctx) -> {
                            String crateName = ctx.getString("crate");
                            int amount = ctx.getInt("amount");

                            Crate crate = plugin.getCrateManager().getCrateIgnoreCase(crateName);
                            if (crate == null) {
                                sender.sendMessage(Colour.colour("&cCrate '" + crateName + "' not found."));
                                return;
                            }

                            if (amount <= 0) {
                                sender.sendMessage(Colour.colour("&cAmount must be positive."));
                                return;
                            }

                            Set<String> ipSet = new HashSet<>();
                            int playersGiven = 0;

                            for (Player player : Bukkit.getOnlinePlayers()) {
                                if (plugin.conf().getBoolean("broadcast.one-per-ip", true)) {
                                    String ip = Objects.requireNonNull(player.getAddress()).getAddress().getHostAddress();
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

                            if (plugin.conf().getBoolean("broadcast.giveall-enabled", true)) {
                                for (String message : plugin.conf().getStringList("broadcast.giveall-message")) {
                                    String formattedMessage = message
                                            .replace("{amount}", String.valueOf(amount))
                                            .replace("{crateName}", crate.getName());
                                    Bukkit.broadcastMessage(Colour.colour(formattedMessage));
                                }
                            }

                            sender.sendMessage(Colour.colour(prefix + " &7Gave &6" + amount + " &7" + crate.getName() +
                                    " key(s) to &6" + playersGiven + " &7players!"));
                        })
                        .build())

                .subCommand(SubCommand.create("list")
                        .description("List all available crates")
                        .permission("crates.list")
                        .execute((sender, ctx) -> {
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

                                String locationStatus;
                                if (crate.getLocation() != null) {
                                    locationStatus = "&aPlaced at " + crate.getLocation().getWorldName() +
                                            " (" + crate.getLocation().getX() + ", " +
                                            crate.getLocation().getY() + ", " +
                                            crate.getLocation().getZ() + ")";
                                } else {
                                    locationStatus = "&7Not placed";
                                }

                                String previewPerm = "crates.preview";
                                String usePerm = "crates.use." + crateName.toLowerCase();

                                sender.sendMessage(Colour.colour("&e" + crateName + " &8(" + displayName + "&8)"));
                                sender.sendMessage(Colour.colour("  &7Status: " + locationStatus));
                                sender.sendMessage(Colour.colour("  &7Preview Permission: &f" + previewPerm));
                                sender.sendMessage(Colour.colour("  &7Use Permission: &f" + usePerm));
                                sender.sendMessage(Colour.colour(""));
                            }

                            sender.sendMessage(Colour.colour("&7Total crates: &e" + crates.size()));
                        })
                        .build())

                .subCommand(SubCommand.create("place")
                        .description("Place a crate at your location")
                        .permission("crates.place")
                        .playerOnly()
                        .argument(CommandArgument.of("crate", ArgumentType.STRING)
                                .description("The crate to place")
                                .required()
                                .completer((sender, partial) -> new ArrayList<>(plugin.getCrateManager().getCrateNames()))
                                .build())
                        .execute((sender, ctx) -> {
                            Player player = (Player) sender;
                            String crateName = ctx.getString("crate");

                            Crate crate = plugin.getCrateManager().getCrateIgnoreCase(crateName);
                            if (crate == null) {
                                sender.sendMessage(Colour.colour("&cCrate '" + crateName + "' not found."));
                                return;
                            }

                            Block targetBlock = player.getLocation().getBlock();

                            if (targetBlock.getType() != Material.AIR) {
                                targetBlock = targetBlock.getRelative(BlockFace.UP);
                            }

                            if (targetBlock.getType() != Material.AIR &&
                                    targetBlock.getType() != Material.GRASS) {
                                player.sendMessage(Colour.colour("&cCannot place crate here - location is obstructed!"));
                                return;
                            }

                            SerializableLocation location = new SerializableLocation(targetBlock.getLocation());

                            SimpleBlock simpleBlock = SimpleBlock.builder().build();
                            simpleBlock.setBlock(targetBlock.getLocation(), crate.getCustomModelData(),
                                    MaterialDb.get(crate.getMaterial()));

                            plugin.setCrateLocation(crate.getName(), location);

                            String prefix = plugin.conf().getString("messages.prefix", "&6[Crates]");
                            String message = plugin.conf().getString("messages.crate-block-placed",
                                    "%prefix% &7%crateType% crate successfully placed at your location.")
                                    .replace("%prefix%", prefix)
                                    .replace("%crateType%", crate.getName());
                            player.sendMessage(Colour.colour(message));
                        })
                        .build())

                .subCommand(SubCommand.create("preview")
                        .description("Preview crate contents")
                        .permission("crates.preview")
                        .playerOnly()
                        .argument(CommandArgument.of("crate", ArgumentType.STRING)
                                .description("The crate to preview")
                                .required()
                                .completer((sender, partial) -> new ArrayList<>(plugin.getCrateManager().getCrateNames()))
                                .build())
                        .execute((sender, ctx) -> {
                            Player player = (Player) sender;
                            String crateName = ctx.getString("crate");

                            Crate crate = plugin.getCrateManager().getCrateIgnoreCase(crateName);
                            if (crate == null) {
                                sender.sendMessage(Colour.colour("&cCrate '" + crateName + "' not found."));
                                return;
                            }

                            new CratePreviewGUI(plugin, player, crate).open();
                        })
                        .build())

                .subCommand(SubCommand.create("reload")
                        .description("Reload crate configurations")
                        .permission("crates.reload")
                        .execute((sender, ctx) -> {
                            long startTime = System.currentTimeMillis();
                            plugin.onReload();
                            long duration = System.currentTimeMillis() - startTime;

                            String prefix = plugin.conf().getString("messages.prefix", "&6[Crates]");
                            sender.sendMessage(Colour.colour(prefix + " &7Plugin reloaded in " + duration + "ms."));
                        })
                        .build())

                .subCommand(SubCommand.create("remove")
                        .description("Remove a placed crate")
                        .permission("crates.remove")
                        .argument(CommandArgument.of("crate", ArgumentType.STRING)
                                .description("The crate to remove")
                                .required()
                                .completer((sender, partial) -> new ArrayList<>(plugin.getCrateManager().getCrateNames()))
                                .build())
                        .execute((sender, ctx) -> {
                            String crateName = ctx.getString("crate");

                            Crate crate = plugin.getCrateManager().getCrateIgnoreCase(crateName);
                            if (crate == null) {
                                sender.sendMessage(Colour.colour("&cCrate '" + crateName + "' not found."));
                                return;
                            }

                            if (crate.getLocation() == null) {
                                sender.sendMessage(Colour.colour("&cCrate '" + crateName + "' doesn't have a location set."));
                                return;
                            }

                            plugin.getCrateManager().removeCrateLocation(crateName);

                            String prefix = plugin.conf().getString("messages.prefix", "&6[Crates]");
                            String message = plugin.conf().getString("messages.crate-removed",
                                    "%prefix% &7%crateType% crate location removed and chest destroyed.")
                                    .replace("%prefix%", prefix)
                                    .replace("%crateType%", crate.getName());
                            sender.sendMessage(Colour.colour(message));
                        })
                        .build())

                .build()
                .register(plugin);
    }
}
