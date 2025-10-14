package net.vulcandev.vulcancrates.listener;

import net.xantharddev.vulcanlib.libs.Colour;
import net.xantharddev.vulcanlib.libs.SerializableLocation;
import net.xantharddev.vulcanlib.libs.material.MaterialDb;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import net.vulcandev.vulcancrates.VulcanCrates;
import net.vulcandev.vulcancrates.event.CrateOpenEvent;
import net.vulcandev.vulcancrates.gui.CratePreviewGUI;
import net.vulcandev.vulcancrates.objects.Crate;
import net.vulcandev.vulcancrates.objects.PlayerData;
import net.vulcandev.vulcancrates.objects.Prize;

/**
 * Listens for player interactions with crate blocks.
 * Left-click opens preview GUI, right-click attempts to open the crate.
 */
public class CrateInteractListener implements Listener {

    private final VulcanCrates plugin;

    public CrateInteractListener(VulcanCrates plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onCrateInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        
        Block block = event.getClickedBlock();
        if (block == null) return;
        
        if (block.getType() != MaterialDb.get(plugin.conf().getString("crate-block.material"))) return;
        
        Player player = event.getPlayer();
        SerializableLocation location = new SerializableLocation(block.getLocation());
        
        // Find crate at this location
        Crate crate = findCrateAtLocation(location);
        if (crate == null) return;

        event.setCancelled(true);


        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            new CratePreviewGUI(plugin, player, crate).open();
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (!playerData.canOpenCrate(crate.getName())) {
                String message = plugin.conf().getString("messages.no-keys").replace("%prefix%", plugin.conf().getString("messages.prefix"));
                player.sendMessage(Colour.colour(message));
                return;
            }
            openCrate(player, crate, playerData);
        }
    }

    private Crate findCrateAtLocation(SerializableLocation location) {
        for (Crate crate : plugin.getCrateManager().getAllCrates()) {
            if (crate.getLocation() != null && crate.getLocation().equals(location)) {
                return crate;
            }
        }

        return null;
    }

    private void openCrate(Player player, Crate crate, PlayerData playerData) {
        playerData.useKey(crate.getName());

        Prize prize = crate.getRandomPrize();
        if (prize == null) {
            player.sendMessage(Colour.colour("&cError: No prizes configured for this crate!"));
            return;
        }

        CrateOpenEvent event = new CrateOpenEvent(player, crate, prize);
        plugin.getServer().getPluginManager().callEvent(event);

        if (prize.getCommands() != null) {
            for (String command : prize.getCommands()) {
                String processedCommand = command.replace("%player%", player.getName());
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), processedCommand);
            }
        }

        String playerMessage = plugin.conf().getString("messages.crate-rolled-reward")
                .replace("%prefix%", plugin.conf().getString("messages.prefix"))
                .replace("%prizeName%", prize.getName())
                .replace("%crateType%", crate.getDisplayName());
        player.sendMessage(Colour.colour(playerMessage));

        if (prize.isAnnounce()) {
            String announceMessage = plugin.conf().getString("messages.crate-rolled-reward-announce")
                    .replace("%prefix%", plugin.conf().getString("messages.prefix"))
                    .replace("%player%", player.getName())
                    .replace("%prizeName%", prize.getName())
                    .replace("%crateType%", crate.getDisplayName());
            plugin.getServer().broadcastMessage(Colour.colour(announceMessage));
        }
    }
}