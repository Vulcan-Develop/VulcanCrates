package net.vulcandev.vulcancrates.command;

import net.vulcandev.vulcancrates.VulcanCrates;
import net.vulcandev.vulcancrates.gui.CratePreviewGUI;
import net.vulcandev.vulcancrates.manager.KeyManager;
import net.vulcandev.vulcancrates.objects.Crate;
import net.vulcandev.vulcancrates.objects.KeyMode;
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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Consolidated crate command handler using VulcanLib's VulcanCommand system.
 * <p>
 * Commands:
 *   /crate give <player> <crate> [amount]           - Give keys using the configured default give mode
 *   /crate givevirtual <player> <crate> [amount]    - Give virtual crate keys to a specific player
 *   /crate givephysical <player> <crate> [amount]   - Give physical crate keys to a specific player
 *   /crate giveall <crate> <amount>                 - Give keys to all online players using the configured default give mode
 *   /crate giveallvirtual <crate> <amount>          - Give virtual crate keys to all online players
 *   /crate giveallphysical <crate> <amount>         - Give physical crate keys to all online players
 *   /crate list                            - List all available crates
 *   /crate place <crate>                   - Place a crate at your location
 *   /crate preview <crate>                 - Preview crate contents in a GUI
 *   /crate reload                          - Reload plugin configuration
 *   /crate remove <crate>                  - Remove a placed crate
 *   /crate check <player> <crate>          - Check how many keys a player has for a specific crate
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

                .subCommand(createGiveSubCommand(plugin, "give",
                        "Give crate keys to a player using the configured default give mode",
                        "crates.give", null))
                .subCommand(createGiveSubCommand(plugin, "givevirtual",
                        "Give virtual crate keys to a player",
                        "crates.give", KeyMode.VIRTUAL))
                .subCommand(createGiveSubCommand(plugin, "givephysical",
                        "Give physical crate keys to a player",
                        "crates.give", KeyMode.PHYSICAL))

                .subCommand(createGiveAllSubCommand(plugin, "giveall",
                        "Give crate keys to all online players using the configured default give mode",
                        "crates.giveall", null))
                .subCommand(createGiveAllSubCommand(plugin, "giveallvirtual",
                        "Give virtual crate keys to all online players",
                        "crates.giveall", KeyMode.VIRTUAL))
                .subCommand(createGiveAllSubCommand(plugin, "giveallphysical",
                        "Give physical crate keys to all online players",
                        "crates.giveall", KeyMode.PHYSICAL))

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
                            sender.sendMessage(Colour.colour("&7Current key mode: &f" +
                                    plugin.getKeyManager().getConfiguredKeyMode().getDisplayName()));
                            sender.sendMessage(Colour.colour("&7Default give mode: &f" +
                                    plugin.getKeyManager().getConfiguredGiveMode().getDisplayName()));
                            if (plugin.getKeyManager().getConfiguredKeyMode() == KeyMode.BOTH) {
                                sender.sendMessage(Colour.colour("&7Consume priority: &f" +
                                        plugin.getKeyManager().getConsumePriorityMode().getDisplayName()));
                            }
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

                .subCommand(SubCommand.create("check")
                        .description("Check how many keys a player has for a specific crate")
                        .permission("crates.check")
                        .argument(CommandArgument.of("player", ArgumentType.PLAYER)
                                .description("The player to check")
                                .required()
                                .build())
                        .argument(CommandArgument.of("crate", ArgumentType.STRING)
                                .description("The type of crate")
                                .required()
                                .completer((sender, partial) -> new ArrayList<>(plugin.getCrateManager().getCrateNames()))
                                .build())
                        .execute((sender, ctx) -> {
                            Player target = ctx.getPlayer("player");
                            String crateName = ctx.getString("crate");

                            Crate crate = plugin.getCrateManager().getCrateIgnoreCase(crateName);
                            if (crate == null) {
                                sender.sendMessage(Colour.colour("&cCrate '" + crateName + "' not found."));
                                return;
                            }

                            String prefix = plugin.conf().getString("messages.prefix", "&6[Crates]");
                            int activeKeyCount = plugin.getKeyManager().getUsableKeys(target, crate);
                            int virtualKeyCount = plugin.getKeyManager().getVirtualKeys(target, crate);
                            int physicalKeyCount = plugin.getKeyManager().getPhysicalKeys(target, crate);
                            String keyType = plugin.getKeyManager().getKeyTypeLabel(plugin.getKeyManager().getConfiguredKeyMode());

                            sender.sendMessage(Colour.colour(prefix + " &e" + target.getName() + " &7has &6" +
                                    activeKeyCount + " &7usable " + crate.getDisplayName() + " &f" + keyType + " &7key(s)."));
                            sender.sendMessage(Colour.colour("&7Virtual: &6" + virtualKeyCount + " &8| &7Physical: &6" + physicalKeyCount));
                        })
                        .build())

                .build()
                .register(plugin);
    }

    private static SubCommand createGiveSubCommand(VulcanCrates plugin, String name, String description,
                                                   String permission, KeyMode forcedMode) {
        return SubCommand.create(name)
                .description(description)
                .permission(permission)
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

                    KeyManager.KeyGrantResult result = plugin.getKeyManager().giveKeys(
                            target,
                            crate,
                            amount,
                            resolveGrantMode(plugin, forcedMode)
                    );
                    sendGrantMessages(plugin, sender, target, crate, result);
                })
                .build();
    }

    private static SubCommand createGiveAllSubCommand(VulcanCrates plugin, String name, String description,
                                                      String permission, KeyMode forcedMode) {
        return SubCommand.create(name)
                .description(description)
                .permission(permission)
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

                    KeyMode grantMode = resolveGrantMode(plugin, forcedMode);
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

                        KeyManager.KeyGrantResult result = plugin.getKeyManager().giveKeys(player, crate, amount, grantMode);
                        sendDroppedPhysicalKeysMessage(plugin, player, crate, result);
                        playersGiven++;
                    }

                    String prefix = plugin.conf().getString("messages.prefix", "&6[Crates]");
                    String keyType = plugin.getKeyManager().getKeyTypeLabel(grantMode);

                    if (plugin.conf().getBoolean("broadcast.giveall-enabled", true)) {
                        for (String message : plugin.conf().getStringList("broadcast.giveall-message")) {
                            String formattedMessage = message
                                    .replace("{amount}", String.valueOf(amount))
                                    .replace("{crateName}", crate.getName())
                                    .replace("{crateDisplayName}", crate.getDisplayName())
                                    .replace("{keyMode}", grantMode.getDisplayName())
                                    .replace("{keyType}", keyType);
                            Bukkit.broadcastMessage(Colour.colour(formattedMessage));
                        }
                    }

                    sender.sendMessage(Colour.colour(prefix + " &7Gave &6" + amount + " &7" +
                            crate.getDisplayName() + " &f" + keyType + " &7key(s) to &6" +
                            playersGiven + " &7players!"));
                })
                .build();
    }

    private static KeyMode resolveGrantMode(VulcanCrates plugin, KeyMode forcedMode) {
        return forcedMode != null ? forcedMode : plugin.getKeyManager().getConfiguredGiveMode();
    }

    private static void sendGrantMessages(VulcanCrates plugin, CommandSender sender, Player target, Crate crate,
                                          KeyManager.KeyGrantResult result) {
        String prefix = plugin.conf().getString("messages.prefix", "&6[Crates]");
        String keyType = result.getKeyTypeLabel();
        int virtualAmount = plugin.getKeyManager().getVirtualKeys(target, crate);
        int physicalAmount = plugin.getKeyManager().getPhysicalKeys(target, crate);

        String gaveMessage = plugin.conf().getString("messages.gave-keys",
                        "%prefix% &7You gave %player% &6%givenAmount% &7%crateType% %keyType% key(s)!")
                .replace("%prefix%", prefix);
        sender.sendMessage(Colour.colour(applyKeyMessagePlaceholders(gaveMessage, target, crate, result,
                virtualAmount, physicalAmount)));

        String receivedMessage = plugin.conf().getString("messages.received-keys",
                        "%prefix% &7You received &6%givenAmount% &7%crateType% %keyType% key(s)!")
                .replace("%prefix%", prefix);
        target.sendMessage(Colour.colour(applyKeyMessagePlaceholders(receivedMessage, target, crate, result,
                virtualAmount, physicalAmount)));

        sendDroppedPhysicalKeysMessage(plugin, target, crate, result);
    }

    private static void sendDroppedPhysicalKeysMessage(VulcanCrates plugin, Player target, Crate crate,
                                                       KeyManager.KeyGrantResult result) {
        if (result.getDroppedAmount() <= 0) {
            return;
        }

        String prefix = plugin.conf().getString("messages.prefix", "&6[Crates]");
        String message = plugin.conf().getString("messages.physical-keys-dropped",
                        "%prefix% &7Your inventory was full, so &6%amount% &7%crateType% physical key(s) were dropped at your feet.")
                .replace("%prefix%", prefix)
                .replace("%amount%", String.valueOf(result.getDroppedAmount()))
                .replace("%crateType%", crate.getDisplayName())
                .replace("%crateName%", crate.getName())
                .replace("%keyType%", result.getKeyTypeLabel());
        target.sendMessage(Colour.colour(message));
    }

    private static String applyKeyMessagePlaceholders(String template, Player target, Crate crate,
                                                      KeyManager.KeyGrantResult result, int virtualAmount,
                                                      int physicalAmount) {
        return template
                .replace("%player%", target.getName())
                .replace("%amount%", String.valueOf(result.getRequestedAmount()))
                .replace("%givenAmount%", String.valueOf(result.getRequestedAmount()))
                .replace("%currentAmount%", String.valueOf(result.getCurrentAmount()))
                .replace("%virtualAmount%", String.valueOf(virtualAmount))
                .replace("%physicalAmount%", String.valueOf(physicalAmount))
                .replace("%crateType%", crate.getDisplayName())
                .replace("%crateName%", crate.getName())
                .replace("%keyMode%", result.getKeyMode().getDisplayName())
                .replace("%keyType%", result.getKeyTypeLabel());
    }
}
