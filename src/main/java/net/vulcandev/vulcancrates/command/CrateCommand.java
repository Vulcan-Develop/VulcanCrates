package net.vulcandev.vulcancrates.command;

import net.vulcandev.vulcancrates.command.subcommand.*;
import net.xantharddev.vulcanlib.command.FeatureCommand;
import net.xantharddev.vulcanlib.libs.Colour;
import org.bukkit.command.CommandSender;
import net.vulcandev.vulcancrates.VulcanCrates;

import java.util.HashSet;
import java.util.Set;

/**
 * Main crate command handler that delegates to various subcommands.
 */
public class CrateCommand implements FeatureCommand {
    private final VulcanCrates plugin;
    private final GiveCommand giveCommand;
    private final GiveAllCommand giveAllCommand;
    private final PlaceCommand placeCommand;
    private final RemoveCommand removeCommand;
    private final ReloadCommand reloadCommand;
    private final PreviewCommand previewCommand;
    private final ListCommand listCommand;

    public CrateCommand(VulcanCrates plugin) {
        this.plugin = plugin;
        this.giveCommand = new GiveCommand(plugin);
        this.giveAllCommand = new GiveAllCommand(plugin);
        this.placeCommand = new PlaceCommand(plugin);
        this.removeCommand = new RemoveCommand(plugin);
        this.reloadCommand = new ReloadCommand(plugin);
        this.previewCommand = new PreviewCommand(plugin);
        this.listCommand = new ListCommand(plugin);
    }

    @Override
    public String getExecutor() {
        return "crates";
    }

    @Override
    public Set<String> getAliases() {
        return new HashSet<>(plugin.conf().getStringList("commands.aliases"));
    }

    @Override
    public String getPermission() {
        return "crates.use";
    }

    @Override
    public boolean requirePlayer() {
        return false;
    }

    @Override
    public void executeCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return;
        }
        
        String subCommand = args[0].toLowerCase();
        String[] subArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subArgs, 0, subArgs.length);
        
        switch (subCommand) {
            case "give":
                giveCommand.execute(sender, subArgs);
                break;
            case "giveall":
                giveAllCommand.execute(sender, subArgs);
                break;
            case "place":
                placeCommand.execute(sender, subArgs);
                break;
            case "remove":
                removeCommand.execute(sender, subArgs);
                break;
            case "reload":
                reloadCommand.execute(sender, subArgs);
                break;
            case "preview":
                previewCommand.execute(sender, subArgs);
                break;
            case "list":
                listCommand.execute(sender, subArgs);
                break;
            case "help":
                sendHelp(sender);
                break;
            default:
                sender.sendMessage(Colour.colour("&cUnknown subcommand. Use /crates help for help."));
                break;
        }
    }

    private void sendHelp(CommandSender sender) {
        String prefix = plugin.conf().getString("messages.prefix", "&6[Crates]");
        sender.sendMessage(Colour.colour(prefix + " &7Available commands:"));
        sender.sendMessage(Colour.colour("&7/crates give <player> <crate> <amount> - Give keys to a player"));
        sender.sendMessage(Colour.colour("&7/crates giveall <crate> <amount> - Give keys to all players"));
        sender.sendMessage(Colour.colour("&7/crates place <crate> - Place a crate at your location"));
        sender.sendMessage(Colour.colour("&7/crates remove <crate> - Remove a crate location"));
        sender.sendMessage(Colour.colour("&7/crates preview <crate> - Preview crate contents"));
        sender.sendMessage(Colour.colour("&7/crates list - List all crates and their permissions"));
        sender.sendMessage(Colour.colour("&7/crates reload - Reload the plugin"));
    }
    
    @Override
    public boolean shouldExecute(String s) {
        return true;
    }
    
    @Override
    public boolean isCommandEnabled() {
        return true;
    }
}